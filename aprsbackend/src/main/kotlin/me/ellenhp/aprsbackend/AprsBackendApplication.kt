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
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprslib.packet.CacheUpdateCommand
import me.ellenhp.aprslib.packet.TimestampedPacket
import me.ellenhp.aprslib.packet.TimestampedSerializedPacket
import me.ellenhp.aprslib.parser.AprsParser
import org.json.JSONArray
import org.json.JSONObject
import java.sql.SQLException
import java.text.DateFormat
import java.util.*

fun Application.main() {

    val config = environment.config.config("sql")
    val isLocal = config.property("isLocal").getString()

    val parser = AprsParser()

    val database = if (isLocal.toBoolean()) {
        DatabaseLayer(null, null, "postgres", "aprs", "localonly")
    } else {
        val dbConnectionString = config.property("dbConnectionString").getString()
        val dbUser = config.property("dbUser").getString()
        val dbName = config.property("dbName").getString()
        val dbPasswordEncrypted = Base64.getDecoder().decode(config.property("dbPasswordEncrypted").getString())
        val client = KeyManagementServiceClient.create()
        val passwordResponse = client.decrypt(CryptoKeyName.of(
                "aprstools",
                "global",
                "api-keys",
                "aprstools-symmetric-key"), ByteString.copyFrom(dbPasswordEncrypted))

        DatabaseLayer(
                dbConnectionString,
                "com.google.cloud.sql.postgres.SocketFactory",
                dbName,
                dbUser,
                passwordResponse.plaintext.toStringUtf8())
    }

    install(DefaultHeaders)
    install(Compression)
    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }

    routing {

        get("/from/{callsign}") {
            call.respondText {
                val callsign = call.parameters["callsign"]
                val ssid = call.parameters["ssid"]
                val packets = database.getAllFrom(Ax25Address(callsign!!, ssid ?: ""))
                JSONArray(packets).toString()
            }
        }

        get("/to/{callsign}") {
            call.respondText {
                val callsign = call.parameters["callsign"]
                val ssid = call.parameters["ssid"]
                val packets = database.getAllTo(Ax25Address(callsign!!, ssid ?: ""))
                JSONArray(packets).toString()
            }
        }

        get("/within/{zone}") {
            val zone = OpenLocationCode.decode(call.parameters["zone"]!!)

            val currentStationsAndTime = database.getStationsIn(zone)

            if (currentStationsAndTime == null) {
                call.respondText {
                    JSONObject(CacheUpdateCommand(
                            true,
                            0,
                            listOf(),
                            listOf())).toString()
                }
            } else {
                call.respondText {
                    JSONObject(CacheUpdateCommand(
                            true,
                            currentStationsAndTime.first,
                            currentStationsAndTime.second,
                            listOf())).toString()
                }
            }

        }

        get("/withinSince/{zone}/{timestampSeconds}") {
            val zone = OpenLocationCode.decode(call.parameters["zone"]!!)

            val currentStationsAndTime = database.getStationsIn(zone)

            if (currentStationsAndTime == null) {
                call.respondText {
                    JSONObject(CacheUpdateCommand(
                            true,
                            0,
                            listOf(),
                            listOf())).toString()
                }
            } else {
                call.respondText {
                    JSONObject(CacheUpdateCommand(
                            true,
                            currentStationsAndTime.first,
                            currentStationsAndTime.second,
                            listOf())).toString()
                }
            }

//            val timestampSeconds = call.parameters["timestampSeconds"]!!.toLong()
//
//            val currentStationsAndTime = database.getStationsIn(zone)
//            if (currentStationsAndTime == null) {
//                call.respondText {
//                    JSONObject(CacheUpdateCommand(true, 0, listOf(), listOf())).toString()
//                }
//            } else {
//                val time = currentStationsAndTime.first
//                val oldPackets = database.getStationsAtTime(zone, timestampSeconds)
//
//                val current = currentStationsAndTime.second.map { timestampedSerialized ->
//                    parser.parse(timestampedSerialized.packet)?.let { TimestampedPacket(timestampedSerialized.millisSinceEpoch, it) }
//                }.filterNotNull().map { it.packet.source to it }.toMap()
//                val old = oldPackets.map { timestampedSerialized ->
//                    parser.parse(timestampedSerialized.packet)?.let { TimestampedPacket(timestampedSerialized.millisSinceEpoch, it) }
//                }.filterNotNull().map { it.packet.source to it }.toMap()
//
//                val newPackets = current.filter { !old.containsValue(it.value) }.map { it.value }.map {
//                    TimestampedSerializedPacket(it.millisSinceEpoch, it.packet.toString())
//                }
//                val stationsToEvict = old.filter { !current.containsKey(it.key) }.map { it.key }
//
//                call.respondText {
//                    JSONObject(CacheUpdateCommand(false, time, newPackets, stationsToEvict)).toString()
//                }
//            }
        }

        post("/uploadpackets") {
            val packets = call.receiveText()
            val packetStrings = JSONArray(packets).map { if (it is String) it else null }.filterNotNull()
            database.putPackets(packetStrings.map { parser.parse(it) }.filterNotNull())
            call.respond(HttpStatusCode.OK, "Thanks!")
        }

        get("/cleanup") {
            database.cleanupPackets()
            call.respond(HttpStatusCode.OK, "Did the thing.")
        }

        get("/setupschema") {
            database.setupSchema()
            call.respond(HttpStatusCode.OK,
                    "Did the thing.")
        }
    }
}
