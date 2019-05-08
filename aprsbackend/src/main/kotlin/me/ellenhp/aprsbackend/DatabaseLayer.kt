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
import javax.sql.DataSource
import javax.sql.rowset.serial.SerialBlob


object Packets: IntIdTable() {
    val fromCallsign = text("fromCallsign").index()
    val toCallsign = text("toCallsign").index()
    val timestamp = datetime("timestamp").index()
    val latLng = point("latLng").nullable().index() // Not all APRS packets have location info
    val packet = blob("packet").index()
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
        config.minimumIdle = 5
        config.connectionTimeout = 10000
        config.idleTimeout = 600000
        config.maxLifetime = 1800000

        pool = HikariDataSource(config)
        Database.connect(getNewConnection = { pool.connection })
    }

    fun putPackets(packets: List<AprsPacket>) {
        val blobs = packets.map { SerialBlob(it.toString().toByteArray(Charsets.ISO_8859_1)) }
        transaction {
            val maxTimestamp = Packets.timestamp.max()
            val blacklist = Packets.slice(maxTimestamp, Packets.packet)
                    .select { Packets.packet inList blobs }
                    .groupBy(Packets.packet)
                    .mapNotNull {
                        if (it[maxTimestamp]?.isBefore(
                                        now().minus(standardSeconds(30)).toDateTime()) == true)
                            it[Packets.packet] else null
                    }

            val newPackets = packets.filter {
                !blacklist.contains(SerialBlob(it.toString().toByteArray(Charsets.ISO_8859_1)))
            }

            Packets.batchInsert(newPackets) {
                this[Packets.fromCallsign] = it.source.call
                this[Packets.toCallsign] = it.dest.call
                this[Packets.timestamp] = DateTime.now()
                it.location()?.let { location -> this[Packets.latLng] = Point(location.longitude, location.latitude) }
                this[Packets.packet] = SerialBlob(it.toString().toByteArray(Charsets.ISO_8859_1))
            }
        }
    }

    fun getAllFrom(callsign: String): List<TimestampedSerializedPacket> {
        val packetRows = transaction {
            Packets.select {
                Packets.fromCallsign eq callsign
            }.toList()
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
            }.toList()
        }
        return packetRows
                .map {
                    val packet = String(it[Packets.packet].binaryStream.readBytes(), Charsets.ISO_8859_1)
                    val timestamp = it[Packets.timestamp].millis
                    TimestampedSerializedPacket(timestamp, packet)
                }
    }

    fun getPacketsNear(southWestCorner: Point, northEastCorner: Point): List<TimestampedSerializedPacket> {
        val packetRows = transaction {
            Packets.select {
                Packets.latLng.isNotNull() and Packets.latLng.within(PGbox2d(southWestCorner, northEastCorner))
            }.limit(500).sortedByDescending {
                Packets.timestamp
            }.toList()
        }
        return packetRows
                .map {
                    val packet = String(it[Packets.packet].binaryStream.readBytes(), Charsets.ISO_8859_1)
                    val timestamp = it[Packets.timestamp].millis
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
            "${expr1.toSQL(queryBuilder)} && ST_MakeEnvelope(${box.llb.x}, ${box.llb.y}, ${box.urt.x}, ${box.urt.y}, 4326)"
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