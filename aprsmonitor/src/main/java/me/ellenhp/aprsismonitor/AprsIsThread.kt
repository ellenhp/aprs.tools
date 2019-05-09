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

import khttp.post
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.parser.AprsParser
import org.json.JSONArray
import java.io.BufferedReader
import java.io.IOException
import java.net.InetSocketAddress
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
        while (!interrupted()) {
            try {
                if (socket?.isConnected != true) {
                    connect()
                }
                readPacket()?.let { packetBuffer.add(it) }
                if (packetBuffer.size >= 100) {
                    val packets = JSONArray(packetBuffer.map { it.toString() })
                    println("Sending packets to server.")
                    post("https://$backendHost/uploadpackets", data = packets, timeout = 10.0)
                    println("Success.")
                    packetBuffer.clear()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun connect() {
        println("Connecting to $host:$port")
        socket = Socket()
        socket?.connect(InetSocketAddress(host, port), 5000)
        // 1000ms looks dangerous at first blush until you remember we're connected to the full feed
        // port which is a firehose of packets at a very consistent 60-70/second. I'd rather roll
        // the round-robin dice again than settle with a poor connection.
        socket?.soTimeout = 1000
        reader = socket?.getInputStream()?.bufferedReader(Charsets.ISO_8859_1)
        val writer = socket?.getOutputStream()?.bufferedWriter()
        writer?.write("user $callsign pass -1\r\n")
        writer?.flush()
        println("Connected to $host:$port")
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