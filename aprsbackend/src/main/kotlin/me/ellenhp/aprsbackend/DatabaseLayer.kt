/*
 * Copyright (c) 2019 Ellen Poe
 *
 * This file is part of APRSTools.
 *
 * APRSTools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * APRSTools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with APRSTools.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.ellenhp.aprsbackend

import com.google.openlocationcode.OpenLocationCode
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.packet.TimestampedSerializedPacket
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.Duration.*
import org.joda.time.Instant
import org.joda.time.Instant.*
import org.postgis.*
import java.sql.Blob
import javax.sql.DataSource
import javax.sql.rowset.serial.SerialBlob


object Packets: IntIdTable() {
    val fromCallsign = text("fromCallsign").index()
    val toCallsign = text("toCallsign").index()
    val timestamp = datetime("timestamp").index()
    val latLng = point("latLng").nullable() // Not all APRS packets have location info
    val packet = blob("packet")
}

class DatabaseLayer(dbConnectionString: String, dbName: String, user: String, password: String) {

    val pool: DataSource

    init {
        val config = HikariConfig()

        config.jdbcUrl = String.format("jdbc:postgresql:///%s", dbName)
        config.username = user
        config.password = password
        config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory")
        config.addDataSourceProperty("cloudSqlInstance", dbConnectionString)
        config.addDataSourceProperty("useSSL", false)
        config.maximumPoolSize = 5
        config.minimumIdle = 2
        config.connectionTimeout = 10000
        config.idleTimeout = 60000
        config.maxLifetime = 1800000

        pool = HikariDataSource(config)
        Database.connect(getNewConnection = { pool.connection })
    }

    fun putPackets(packets: List<AprsPacket>) {
        val blobs: Map<AprsPacket, Blob> =
                packets.map { it to SerialBlob(it.toString().toByteArray(Charsets.ISO_8859_1)) }.toMap()
        transaction {
            val blacklist = Packets.select {
                Packets.timestamp.greaterEq(now().minus(standardSeconds(30)).toDateTime()) and
                Packets.toCallsign.inList(packets.map { it.source.call }) and
                Packets.packet.inList(blobs.values)
            }.map { it[Packets.packet] }

            val newPackets = packets.filter {
                !blacklist.contains(blobs[it])
            }

            Packets.batchInsert(newPackets) {
                this[Packets.fromCallsign] = it.source.call
                this[Packets.toCallsign] = it.dest.call
                this[Packets.timestamp] = DateTime.now()
                it.location()?.let { location -> this[Packets.latLng] = Point(location.longitude, location.latitude) }
                this[Packets.packet] = blobs[it] ?: error("No blob found, this shouldn't happen")
            }
        }
    }

    fun getAllFrom(callsign: String): List<TimestampedSerializedPacket> {
        val packetRows = transaction {
            Packets.select {
                Packets.fromCallsign eq callsign
            }.orderBy(Packets.timestamp, SortOrder.DESC).limit(500).toList()
        }
        return packetRows
                .map {
                    val packet = String(it[Packets.packet].binaryStream.readBytes(), Charsets.ISO_8859_1)
                    val timestamp = it[Packets.timestamp].millis
                    TimestampedSerializedPacket(timestamp, packet)
                }
    }

    fun getAllTo(callsign: String): List<TimestampedSerializedPacket> {
        val packetRows = transaction {
            Packets.select {
                Packets.toCallsign eq callsign
            }.orderBy(Packets.timestamp, SortOrder.DESC).limit(500).toList()
        }
        return packetRows
                .map {
                    val packet = String(it[Packets.packet].binaryStream.readBytes(), Charsets.ISO_8859_1)
                    val timestamp = it[Packets.timestamp].millis
                    TimestampedSerializedPacket(timestamp, packet)
                }
    }

    fun getPacketsNear(codeAreas: List<OpenLocationCode.CodeArea>): List<TimestampedSerializedPacket> {
        val packetRows = transaction {
            Packets.select {
                val isInCodeAreas = codeAreas.map {
                    Packets.latLng.within(
                            PGbox2d(Point(it.westLongitude, it.southLatitude),
                                    Point(it.eastLongitude, it.northLatitude)))
                }.reduce { arg1, arg2 ->
                    arg1 or arg2
                }
                Packets.latLng.isNotNull() and isInCodeAreas
            }.orderBy(Packets.timestamp, SortOrder.DESC).limit(250).toList()
        }
        return packetRows
                .map {
                    val packetBlob = it[Packets.packet]
                    // Blob.getBytes uses 1-based indexing.
                    val packet = String(packetBlob.getBytes(1, packetBlob.length().toInt()), Charsets.ISO_8859_1)
                    val timestamp = it[Packets.timestamp].millis
                    packetBlob.free()
                    TimestampedSerializedPacket(timestamp, packet)
                }
    }

    fun cleanupPackets(expiryTime: Instant) {
        transaction {
            Packets.deleteWhere {
                Packets.timestamp less expiryTime.toDateTime()
            }
        }
    }

    fun setupSchema() {
        transaction {
            SchemaUtils.create(Packets)
        }
    }
}

// We need a bunch of extension functions to do geospatial queries.

fun Table.point(name: String): Column<Point>
        = registerColumn(name, PointColumnType())

infix fun ExpressionWithColumnType<*>.within(box: PGbox2d) : Op<Boolean>
        = WithinOp(this, box)

private class WithinOp(val expr1: Expression<*>, val box: PGbox2d) : Op<Boolean>() {
    override fun toSQL(queryBuilder: QueryBuilder) =
            "ST_Contains(ST_MakeEnvelope(${box.llb.x}, ${box.llb.y}, ${box.urt.x}, ${box.urt.y}, 4326)," +
            "${expr1.toSQL(queryBuilder)})"
}

private class PointColumnType(): ColumnType() {
    override fun sqlType() = "GEOMETRY(Point, 4326)"
    override fun valueFromDB(value: Any) = if (value is PGgeometry) value.geometry else value
    override fun notNullValueToDB(value: Any): Any {
        if (value is Point) {
            if (value.srid == Point.UNKNOWN_SRID) value.srid = 4326
            return PGgeometry(value)
        }
        return value
    }
}