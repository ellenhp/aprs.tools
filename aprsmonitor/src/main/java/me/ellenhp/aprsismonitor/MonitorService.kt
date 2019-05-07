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
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int

class BackendService : CliktCommand() {
    private val aprsIsHost: String by option(help="The APRS-IS server to connect to")
            .default("rotate.aprs.net")
    private val aprsIsPort: Int by option(help="The APRS-IS port to connect to").int()
            .default(10152)
    private val callsign: String by option(help="The callsign to use when connecting to APRS-IS")
            .required()
    private val backendHost: String by option(help="The callsign to use when connecting to APRS-IS")
            .required()

    override fun run() {
        println("Creating APRS-IS backend service.")
        val thread = AprsIsThread(aprsIsHost, aprsIsPort, callsign, backendHost)
        println("Starting APRS-IS backend service.")
        thread.start()
        thread.join()
    }
}

fun main(args: Array<String>) = BackendService().main(args)