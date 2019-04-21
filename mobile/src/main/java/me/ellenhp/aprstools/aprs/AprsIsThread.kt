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

import android.util.Log
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprstools.history.PacketTrackHistory
import java.io.IOException
import java.time.Instant
import java.util.*
import javax.inject.Provider

class AprsIsThread(val packetTrackHistory: PacketTrackHistory, val instantProvider: Provider<Instant>) : Thread() {

    private val TAG = this::class.java.simpleName
    private val BACKOFF_PERIOD_MILLIS = 2_000L

    private var client: AprsIsClient? = null
    private var shouldExit = false

    private val queue: Queue<AprsPacket> = ArrayDeque()

    fun setClient(newClient: AprsIsClient) {
        client?.disconnect()
        client = newClient
    }

    @Synchronized
    fun enqueuePacket(aprsPacket: AprsPacket) {
        queue.offer(aprsPacket)
    }

    override fun run() {
        while (!shouldExit) {
            doRead()
            doWrite()
        }
    }

    private fun doRead() {
        val packet = readPacket()
        if (packet == null) {
            backoff()
            return
        }
        packetTrackHistory.add(packet, instantProvider.get())
    }

    @Synchronized
    private fun doWrite() {
        client?.writePacket(queue.poll() ?: return)
    }

    private fun readPacket(): AprsPacket? {
        val client = client ?: return null
        try {
            return client.readPacket()
        } catch (e: IOException) {
            Log.w(TAG, "Couldn't read a packet for some reason.", e)
            return null
        }
    }

    private fun backoff() {
        try {
            Thread.sleep(BACKOFF_PERIOD_MILLIS);
        } catch (e: InterruptedException) {
            shouldExit = true
        }
    }
}