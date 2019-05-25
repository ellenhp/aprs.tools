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

import java.time.Duration
import java.time.Instant
import java.time.Instant.now

class Watchdog(val timeout: Duration) {

    private var lastHeartbeat: Instant? = null
    private val lock = Object()
    private val monitor = Object()

    fun waitUntilDead() {
        while (!Thread.interrupted()) {
            synchronized(lock) {
                val heartbeatSnapshot = lastHeartbeat
                if (heartbeatSnapshot != null && heartbeatSnapshot.isBefore(now().minus(timeout))) {
                    return
                }
            }
            synchronized(monitor) {
                monitor.wait(timeout.toMillis())
            }
        }
    }

    fun heartbeat() {
        synchronized(lock) {
            lastHeartbeat = now()
        }
        synchronized(monitor) {
            monitor.notify()
        }
    }
}