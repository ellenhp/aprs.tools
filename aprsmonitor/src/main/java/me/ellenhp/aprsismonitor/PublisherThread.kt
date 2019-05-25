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
import org.json.JSONArray
import java.io.IOException

class PublisherThread(
    private val backendHost: String,
    private val useSSL: Boolean
) : Thread() {

    private val queue = ArrayList<AprsPacket>()
    private val lock = Object()

    override fun run() {
        while (!interrupted()) {
            synchronized(lock) {
                lock.wait()
                try {
                    val packets = JSONArray(queue.map { it.toString() })
                    println("Sending packets to server.")
                    val url = "http${if (useSSL) "s" else ""}://$backendHost/uploadpackets"
                    val response = post(url, data = packets, timeout = 10.0)
                    println("Response: ${response.statusCode}")
                } catch (e: IOException) {
                    // Swallow the exception but be sure to clear the queue either way so we don't hammer the server in a busy loop.
                    e.printStackTrace()
                } finally {
                    queue.clear()
                }
            }
        }
        super.run()
    }

    fun appendPacket(packet: AprsPacket) {
        synchronized(lock) {
            queue.add(packet)
            if (queue.size > 100) {
                lock.notify()
            }
        }
    }
}