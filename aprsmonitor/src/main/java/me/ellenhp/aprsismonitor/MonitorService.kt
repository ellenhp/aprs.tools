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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import java.time.Duration

class BackendService : CliktCommand() {
    private val aprsIsHost: String by option(help = "The APRS-IS server to connect to")
            .default("rotate.aprs.net")
    private val aprsIsPort: Int by option(help = "The APRS-IS port to connect to").int()
            .default(10152)
    private val callsign: String by option(help = "The callsign to use when connecting to APRS-IS")
            .required()
    private val backendHost: String by option(help = "The callsign to use when connecting to APRS-IS")
            .required()
    private val useSSL: Boolean by option("--use-ssl", help = "Whether or not to use SSL")
            .flag("--disable-ssl", default = true)

    override fun run() {
        while (!Thread.interrupted()) {
            println("Creating uploader thread")
            val publisher = PublisherThread(backendHost, useSSL)

            println("Creating watchdog.")
            val watchdog = Watchdog(Duration.ofSeconds(10))

            println("Creating APRS-IS thread.")
            val aprsIsThread = AprsIsThread(aprsIsHost, aprsIsPort, callsign, publisher, watchdog)

            println("Starting monitor service.")
            publisher.start()
            aprsIsThread.start()

            watchdog.waitUntilDead()

            println("Watchdog activated, cleaning up")

            publisher.interrupt()
            aprsIsThread.interrupt()
        }
    }
}

fun main(args: Array<String>) = BackendService().main(args)