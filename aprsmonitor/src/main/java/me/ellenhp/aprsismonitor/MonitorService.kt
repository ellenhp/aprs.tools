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
        val thread = AprsIsThread(aprsIsHost, aprsIsPort, callsign)
        println("Starting APRS-IS backend service.")
        thread.start()
        thread.join()
    }
}

fun main(args: Array<String>) = BackendService().main(args)