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

package me.ellenhp.aprsismonitor

import com.google.gson.JsonArray
import khttp.post
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.parser.AprsParser
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.Socket

class AprsIsThread(private val host: String,
                   private val port: Int,
                   private val callsign: String,
                   private val backendHost: String): Thread() {

    private val parser = AprsParser()
    private val packetBuffer = ArrayList<AprsPacket>()

    private var socket: Socket? = null
    private var reader: BufferedReader? = null

    override fun run() {
        println("Starting APRS-IS thread.")
        while (!Thread.interrupted()) {
            if (socket?.isConnected != true) {
                print("Connecting to $host:$port")
                connect()
            }
            readPacket()?.let { packetBuffer.add(it) }
            if (packetBuffer.size >= 200) {
                val packets = JSONArray(packetBuffer.map { it.toString() })

                println(packets)

                post("https://$backendHost/uploadpackets", data=packets)
                packetBuffer.clear()
            }
        }
    }

    private fun connect() {
        socket = Socket(host, port)
        reader = socket?.getInputStream()?.bufferedReader(Charsets.ISO_8859_1)
        val writer = socket?.getOutputStream()?.bufferedWriter()
        writer?.write("user $callsign pass -1\r\n")
        writer?.flush()
    }

    private fun readPacket(): AprsPacket? {
        val rawPacket = reader?.readLine()?.trim() ?: return null
        return try {
            val packet = parser.parse(rawPacket)
            packet
        } catch (e: IOException) {
            null
        }
    }
}