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
import me.ellenhp.aprslib.parser.AprsParser
import org.joda.time.Duration.*
import org.joda.time.Instant.now
import org.json.JSONArray
import org.postgis.Point
import java.text.DateFormat
import java.util.*

fun Application.init() {

}

fun Application.main() {

    val config = environment.config.config("sql")
    val dbConnectionString = config.property("dbConnectionString").getString()
    val dbUser = config.property("dbUser").getString()
    val dbName = config.property("dbName").getString()
    val dbPasswordEncrypted = Base64.getDecoder().decode(config.property("dbPasswordEncrypted").getString())

    val parser = AprsParser()

    val client = KeyManagementServiceClient.create()
    val passwordResponse = client.decrypt(CryptoKeyName.of(
                    "aprstools",
                    "global",
                    "api-keys",
                    "aprstools-symmetric-key"), ByteString.copyFrom(dbPasswordEncrypted))

    val database = DatabaseLayer(dbConnectionString, dbName, dbUser, passwordResponse.plaintext.toStringUtf8())

    // Warm the database connection pool by making a meaningless request.
    database.getAllFrom("")

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
                val packets = database.getAllFrom(callsign!!)
                JSONArray(packets).toString()
            }
        }

        get("/to/{callsign}") {
            call.respondText {
                val callsign = call.parameters["callsign"]
                val packets = database.getAllTo(callsign!!)
                JSONArray(packets).toString()
            }
        }

        get("/within/{plusCodes}/") {
            val plusCodes = call.parameters["plusCodes"]!!.toString().split(",")
            val packets = database.getPacketsNear(plusCodes.map { OpenLocationCode.decode(it) })
            call.respondText {
                JSONArray(packets).toString()
            }
        }

        post("/uploadpackets") {
            val packets = call.receiveText()
            val packetStrings = JSONArray(packets).map { if (it is String) it else null }.filterNotNull()
            database.putPackets(packetStrings.map { parser.parse(it) }.filterNotNull())
            call.respond(HttpStatusCode.OK, "Thanks!")
        }

        get("/cleanup") {
            database.cleanupPackets(now().minus(standardHours(6)))
            call.respond(HttpStatusCode.OK, "Did the thing.")
        }

        get("/setupschema") {
            database.setupSchema()
            call.respond(HttpStatusCode.OK,
                    "Don't forget to create an index on latLng using PostGIS!")
        }
    }
}
