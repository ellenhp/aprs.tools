package me.ellenhp.aprsismonitor

import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.parser.AprsParser
import java.io.BufferedReader
import java.io.IOException
import java.net.Socket

class AprsIsThread(private val host: String,
                   private val port: Int,
                   private val callsign: String): Thread() {

    private val parser = AprsParser()
    private var packetBuffer = ArrayList<AprsPacket>()

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