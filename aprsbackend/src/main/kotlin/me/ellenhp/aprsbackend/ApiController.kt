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

import com.google.gson.Gson
import com.google.openlocationcode.OpenLocationCode
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprslib.packet.CacheUpdateCommand
import me.ellenhp.aprslib.packet.CacheUpdateCommandPosits
import me.ellenhp.aprslib.packet.TimestampedPosit
import me.ellenhp.aprslib.parser.AprsParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit

@RestController
class ApiController @Autowired constructor(private val databaseLayer: DatabaseLayer) {

    @GetMapping("/")
    fun index(): String {
        return "Welcome, please don't DoS my APRS endpoints, lol. Email me at ellen.h.poe@gmail.com " +
                "if you want to use this API so we can talk about whether or not it can handle the " +
                "load. You can find code at https://github.com/ellenpoe/aprs.tools"
    }

    @GetMapping("/from/{callsign}")
    fun from(@PathVariable(value="callsign") callsign: String,
             @RequestParam(defaultValue = "") ssid: String): ResponseEntity<String> {
        val packets = databaseLayer.getAllFrom(Ax25Address(callsign, ssid))
        val json = Gson().toJson(packets)
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                .body(json);
    }

    @GetMapping("/to/{callsign}")
    fun to(@PathVariable(value="callsign") callsign: String,
           @RequestParam(defaultValue = "") ssid: String): ResponseEntity<String> {
        val packets = databaseLayer.getAllTo(Ax25Address(callsign, ssid))
        val json = Gson().toJson(packets)
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                .body(json);
    }

    @GetMapping("/within/{plusCode}")
    fun within(@PathVariable(value="plusCode") plusCode: String,
               @RequestParam(defaultValue = "") type: String): ResponseEntity<String> {
        val zone = OpenLocationCode.decode(plusCode)
        val packets = databaseLayer.getPacketsIn(zone)?.second ?: listOf()
        val json = if (type == "" || type == "packets") {
            val cacheUpdate = CacheUpdateCommand(true, 0,
                    packets, listOf())
            Gson().toJson(cacheUpdate)
        } else {
            val parser = AprsParser()
            val timestampedPosits = packets.mapNotNull { timestampedPacket ->
                val packet = parser.parse(timestampedPacket.packet) ?: return@mapNotNull null
                val location = packet.location() ?: return@mapNotNull null
                val symbol = packet.symbol() ?: return@mapNotNull null
                val locationCode = OpenLocationCode(location.latitude, location.longitude)
                TimestampedPosit(timestampedPacket.millisSinceEpoch,
                        packet.source, locationCode.code, symbol)
            }
            val cacheUpdate = CacheUpdateCommandPosits(true, 0,
                    timestampedPosits ?: listOf(), listOf())
            Gson().toJson(cacheUpdate)
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(120, TimeUnit.SECONDS))
                .body(json);
    }

    @GetMapping("/cleanup")
    fun delete(): String {
        databaseLayer.cleanupPackets()
        return "Did the thing"
    }

    @GetMapping("/setupschema")
    fun setupSchema(): String {
        databaseLayer.setupSchema()
        return "Did the thing"
    }

    @PostMapping("/uploadpackets")
    fun uploadPackets(@RequestBody request: String) {
        val parser = AprsParser()
        val packetStrings = Gson().fromJson(request, Array<String>::class.java)
        val packets = packetStrings.map { parser.parse(it) }.filterNotNull()
        databaseLayer.putPackets(packets)
    }

}
