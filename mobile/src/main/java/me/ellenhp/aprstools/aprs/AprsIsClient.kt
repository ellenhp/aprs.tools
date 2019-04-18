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

package me.ellenhp.aprstools.aprs

import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.parser.AprsParser
import java.io.*
import java.net.Socket

class AprsIsClient(private val host: String,
                   private val port: Int,
                   private val callsign: String,
                   private val passcode: String?,
                   private val filter: LocationFilter?) {

    private val parser = AprsParser()

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    private var isInitialized = false

    @Synchronized
    @Throws(IOException::class)
    fun readPacket(): AprsPacket? {
        if (!isInitialized)
            init()
        val rawPacket = reader?.readLine()?.trim() ?: return null
        return parser.parse(rawPacket)
    }

    @Synchronized
    @Throws(IOException::class)
    fun writePacket(packet: AprsPacket) {
        if (!isInitialized)
            init()
        writer?.write(packet.toString() + "\r\n")
        writer?.flush()
    }

    fun disconnect() {
        socket?.close()
    }

    private fun init() {
        socket = Socket(host, port)
        reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
        writer = BufferedWriter(OutputStreamWriter(socket?.getOutputStream()))

        writer?.write(String.format("user %s pass %s\r\n", callsign, passcode ?: "-1"))
        writer?.write("filter default\r\n")
        if (filter != null)
            writer?.write(filter.filterCommand)
        writer?.flush()

        isInitialized = true
    }

    private fun authenticate() {
    }

    companion object {
        private val TAG = AprsIsClient::class.java.name
    }
}
