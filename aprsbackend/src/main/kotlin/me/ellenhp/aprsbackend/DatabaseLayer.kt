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

import com.google.cloud.kms.v1.CryptoKeyName
import com.google.cloud.kms.v1.KeyManagementServiceClient
import com.google.openlocationcode.OpenLocationCode
import com.google.protobuf.ByteString
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprslib.packet.TimestampedSerializedPacket
import me.ellenhp.aprslib.parser.AprsParser
import org.postgis.PGgeometry
import org.postgis.Point
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.sql.Connection
import java.util.Base64
import javax.sql.DataSource
import kotlin.collections.ArrayList

@Component
class DatabaseLayer {

    @Value("\${databaseProperties.connectionString}")
    var dbConnectionString: String? = null
    @Value("\${databaseProperties.socketFactory}")
    var socketFactory: String? = null
    @Value("\${databaseProperties.name}")
    var dbName: String? = null
    @Value("\${databaseProperties.user}")
    var user: String? = null
    @Value("\${databaseProperties.password}")
    var encryptedPasswordBase64: String? = null

    private var pool: DataSource? = null
    get() {
        if (field == null) {
            val dbPasswordEncrypted = Base64.getDecoder()
                    .decode(encryptedPasswordBase64)
            val client = KeyManagementServiceClient.create()
            val passwordResponse = client.decrypt(CryptoKeyName.of(
                "aprstools",
                "global",
                "api-keys",
                "aprstools-symmetric-key"), ByteString.copyFrom(dbPasswordEncrypted))

            val config = HikariConfig()

            config.jdbcUrl = String.format("jdbc:postgresql:///%s", dbName)
            config.username = user
            config.password = passwordResponse.plaintext.toString(Charsets.UTF_8)
            socketFactory?.let { config.addDataSourceProperty("socketFactory", it) }
            dbConnectionString?.let { config.addDataSourceProperty("cloudSqlInstance", it) }
            config.addDataSourceProperty("useSSL", false)
            config.maximumPoolSize = 20
            config.minimumIdle = 1
            config.connectionTimeout = 10000
            config.idleTimeout = 600000
            config.maxLifetime = 1800000

            field = HikariDataSource(config)
        }
        return field
    }

    fun putPackets(packets: List<AprsPacket>) {
        val conn = pool!!.connection
        conn.autoCommit = false

        try {
            insertAllStations(conn, packets.map { it.source })
            insertAllStations(conn, packets.map { it.dest })
            conn.commit()
            insertAllPackets(conn, packets)
            conn.commit()
        } finally {
            conn.autoCommit = true
            conn.close()
        }
    }

    fun getAllFrom(station: Ax25Address): List<TimestampedSerializedPacket> {
        pool!!.connection.use { conn ->
            val select = conn.prepareStatement("""
                SELECT packet, received_timestamp, source_call, source_ssid FROM packets WHERE source_call=? AND source_ssid=?;
            """.trimIndent())
            select.fetchSize = 500
            select.setString(1, station.call)
            select.setString(2, station.ssid)
            val results = select.executeQuery()
            val packets = ArrayList<TimestampedSerializedPacket>()
            while (results.next()) {
                val blob = results.getBytes(1)
                packets.add(TimestampedSerializedPacket(
                        results.getTimestamp(2).time,
                        String(blob, Charsets.ISO_8859_1)))
            }
            return packets.toList()
        }
    }

    fun getAllTo(station: Ax25Address): List<TimestampedSerializedPacket> {
        pool!!.connection.use { conn ->
            val select = conn.prepareStatement("""
                SELECT packet, received_timestamp, dest_call, dest_ssid FROM packets WHERE dest_call=? AND dest_ssid=?;
            """.trimIndent())
            select.fetchSize = 500
            select.setString(1, station.call)
            select.setString(2, station.ssid)
            val results = select.executeQuery()
            val packets = ArrayList<TimestampedSerializedPacket>()
            while (results.next()) {
                val blob = results.getBytes(1)
                packets.add(TimestampedSerializedPacket(
                        results.getTimestamp(2).time,
                        String(blob, Charsets.ISO_8859_1)))
            }
            return packets.toList()
        }
    }

    fun getPacketsIn(codeArea: OpenLocationCode.CodeArea): Pair<Long, List<TimestampedSerializedPacket>>? {
        pool!!.connection.use { conn ->
            val select = conn.prepareStatement("""
                SELECT packets.packet, packets.received_timestamp, NOW()
                FROM latestPackets
                JOIN packets ON latestPackets.packet=packets.id
                WHERE packets.latlng && ST_MakeEnvelope(?, ?, ?, ?)
                AND packets.received_timestamp >= NOW() - INTERVAL '1 hour';
             """.trimIndent())
            select.fetchSize = 500
            select.setDouble(1, codeArea.westLongitude)
            select.setDouble(2, codeArea.southLatitude)
            select.setDouble(3, codeArea.eastLongitude)
            select.setDouble(4, codeArea.northLatitude)
            val results = select.executeQuery()
            val packets = ArrayList<TimestampedSerializedPacket>()
            var now: Long? = null
            while (results.next()) {
                val blob = results.getBytes(1)
                packets.add(TimestampedSerializedPacket(
                        results.getTimestamp(2).time,
                        String(blob, Charsets.ISO_8859_1)))
                now = now ?: results.getTimestamp(3).time / 1000
            }
            packets.mapNotNull { AprsParser().parse(it.packet)?.location() }.forEach {
                if (it.latitude < codeArea.southLatitude || it.latitude > codeArea.northLatitude)
                    error("latitude out of bounds")
                if (it.longitude < codeArea.westLongitude || it.longitude > codeArea.eastLongitude)
                    error("{$it.longitude} out of bounds")
            }
            return now?.let { it to packets.toList() }
        }
    }

    fun cleanupPackets() {
        val conn = pool!!.connection
        val delete = conn.prepareStatement("""
            DELETE FROM packets WHERE packets.received_timestamp < NOW() - INTERVAL '24 hours';
        """.trimIndent())
        delete.executeUpdate()
        conn.close()
    }

    fun setupSchema() {
        pool!!.connection.use { conn ->
            val setupSchemaStatement = conn.prepareStatement("""
                CREATE EXTENSION IF NOT EXISTS postgis;
                CREATE EXTENSION IF NOT EXISTS btree_gist;

                CREATE TABLE IF NOT EXISTS stations (
                    callsign text,
                    ssid text,
                    track geometry(LINESTRING),
                    PRIMARY KEY (callsign, ssid));

                CREATE TABLE IF NOT EXISTS packets (
                    id BIGSERIAL PRIMARY KEY,
                    source_call text,
                    source_ssid text,
                    dest_call text,
                    dest_ssid text,
                    received_timestamp timestamptz,
                    latlng geometry(POINT),
                    packet bytea,
                    FOREIGN KEY (source_call, source_ssid) REFERENCES stations,
                    FOREIGN KEY (dest_call, dest_ssid) REFERENCES stations,
                    CONSTRAINT possible_duplicate EXCLUDE USING GIST (
                        packet WITH =,
                        tsrange(received_timestamp AT TIME ZONE 'UTC', received_timestamp AT TIME ZONE 'UTC' + INTERVAL '30 seconds') WITH &&
                    ));

                CREATE TABLE IF NOT EXISTS latestPackets (
                    callsign text,
                    ssid text,
                    packet bigint REFERENCES packets ON DELETE CASCADE,
                    PRIMARY KEY (callsign, ssid),
                    FOREIGN KEY (callsign, ssid) REFERENCES stations);


                CREATE INDEX ON stations(callsign);
                CREATE INDEX ON stations USING gist (track);

                CREATE INDEX ON packets(source_call);
                CREATE INDEX ON packets(dest_call);
                CREATE INDEX ON packets(source_ssid);
                CREATE INDEX ON packets(dest_ssid);
                CREATE INDEX ON packets(received_timestamp);
                CREATE INDEX ON packets(packet);
                CREATE INDEX ON packets USING gist (latlng);

                CREATE INDEX ON latestPackets(callsign);
                CREATE INDEX ON latestPackets(ssid);

            """.trimIndent())
            setupSchemaStatement.executeUpdate()
        }
    }

    private fun insertAllStations(conn: Connection, stations: List<Ax25Address>) {
        val statement = conn.prepareStatement(
                "INSERT INTO stations(callsign, ssid, track) VALUES(?, ?, null) ON CONFLICT DO NOTHING;")
        for (station in stations) {
            statement.setString(1, station.call)
            statement.setString(2, station.ssid ?: "")
            statement.addBatch()
        }
        statement.executeBatch()
    }

    private fun insertAllPackets(conn: Connection, packets: List<AprsPacket>) {
        val insertPackets = conn.prepareStatement(
                "INSERT INTO packets(" +
                        "source_call," +
                        "source_ssid," +
                        "dest_call," +
                        "dest_ssid," +
                        "received_timestamp," +
                        "latlng," +
                        "packet)" +
                        "VALUES(?, ?, ?, ?, NOW(), ?, ?)" +
                        "ON CONFLICT DO NOTHING;", arrayOf("id"))
        for (packet in packets) {
            insertPackets.setString(1, packet.source.call)
            insertPackets.setString(2, packet.source.ssid ?: "")
            insertPackets.setString(3, packet.dest.call)
            insertPackets.setString(4, packet.dest.ssid ?: "")
            insertPackets.setObject(5, packet.location()?.let {
                val point = Point(it.longitude, it.latitude)
                PGgeometry(point)
            })
            insertPackets.setBytes(6, packet.toString().toByteArray(Charsets.ISO_8859_1))
            insertPackets.addBatch()
        }
        insertPackets.executeBatch()

        val upsertLatest = conn.prepareStatement(
                "INSERT INTO latestPackets(" +
                        "callsign," +
                        "ssid," +
                        "packet) " +
                        "(" +
                        "   SELECT source_call, source_ssid, id FROM packets WHERE id=?" +
                        ")" +
                        "ON CONFLICT (callsign, ssid) DO UPDATE SET (packet) = (EXCLUDED.packet);")

        val keys = insertPackets.generatedKeys
        var indexIntoPackets = 0
        while (keys.next()) {
            val currentPacket = packets[indexIntoPackets++]
            val key = keys.getLong(1)
            if (currentPacket.location() == null) {
                continue
            }
            upsertLatest.setLong(1, key)
            upsertLatest.addBatch()
        }
        upsertLatest.executeBatch()
    }
}