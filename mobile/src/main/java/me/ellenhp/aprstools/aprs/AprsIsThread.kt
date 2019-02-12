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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.ellenhp.aprstools.aprs

import android.util.Log
import me.ellenhp.aprslib.packet.AprsPacket
import java.io.IOException

class AprsIsThread(var listener: AprsIsListener?): Thread() {

    private val TAG = this::class.java.simpleName
    private val BACKOFF_PERIOD_MILLIS = 2_000L

    private var client: AprsIsClient? = null
    private var shouldExit = false

    fun setClient(newClient: AprsIsClient) {
        client?.disconnect()
        client = newClient
    }

    override fun run() {
        while (!shouldExit) {
            val packet = readPacket()
            if (packet == null) {
                backoff()
                continue
            }
            listener?.onAprsPacketReceived(packet)
        }
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