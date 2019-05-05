package me.ellenhp.aprsbackend

import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.parser.AprsParser
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgis.*
import javax.sql.rowset.serial.SerialBlob


object Packets: IntIdTable() {
    val fromCallsign = varchar("fromCallsign", 20).index()
    val toCallsign = varchar("toCallsign", 20).index()
    val millisSinceEpoch = long("timestamp")
    val latLng = point("latLng").nullable() // Not all APRS packets have location info
    val packet = blob("packet")
}

class DatabaseLayer(dbConnectionString: String, user: String, password: String) {

    val parser = AprsParser()

    init {
        Database.connect(dbConnectionString, "org.postgresql.Driver", user, password)
    }

    fun putPackets(packets: List<AprsPacket>) {
        transaction {
            SchemaUtils.create(Packets)
            Packets.batchInsert(packets) {
                this[Packets.fromCallsign] = it.source.call
                this[Packets.toCallsign] = it.dest.call
                this[Packets.millisSinceEpoch] = System.currentTimeMillis() // TODO this isn't testable
                it.location()?.let { location -> this[Packets.latLng] = Point(location.longitude, location.latitude) }
                this[Packets.packet] = SerialBlob(it.toString().toByteArray(Charsets.ISO_8859_1))
            }
        }
    }

    fun getAllFrom(callsign: String): List<AprsPacket> {
        val packetRows = transaction {
            Packets.select {
                Packets.fromCallsign eq callsign
            }.toList()
        }
        return packetRows
                .map { it[Packets.packet].binaryStream.readBytes() }
                .map { String(it, Charsets.ISO_8859_1) }
                .map { parser.parse(it) }
                .filterNotNull()
    }

    fun getAllTo(callsign: String): List<AprsPacket> {
        val packetRows = transaction {
            Packets.select {
                Packets.toCallsign eq callsign
            }.toList()
        }
        return packetRows
                .map { it[Packets.packet].binaryStream.readBytes() }
                .map { String(it, Charsets.ISO_8859_1) }
                .map { parser.parse(it) }
                .filterNotNull()
    }

    fun getPacketsNear(southWestCorner: Point, northEastCorner: Point): List<AprsPacket> {
        val packetRows = transaction {
            Packets.select {
                Packets.latLng.isNotNull() and Packets.latLng.within(PGbox2d(southWestCorner, northEastCorner))
            }.toList()
        }
        return packetRows
                .map { it[Packets.packet].binaryStream.readBytes() }
                .map { String(it, Charsets.ISO_8859_1) }
                .map { parser.parse(it) }
                .filterNotNull()
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