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

package me.ellenhp.aprstools.history

import com.google.android.gms.maps.model.LatLng
import com.google.openlocationcode.OpenLocationCode
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprslib.packet.CacheUpdateCommand
import me.ellenhp.aprslib.packet.TimestampedPacket
import me.ellenhp.aprslib.parser.AprsParser
import me.ellenhp.aprstools.map.PacketPlotter
import org.threeten.bp.Duration
import org.threeten.bp.Instant

class PacketCacheCell(val cell: OpenLocationCode) {

    var next: PacketCacheCell? = null
    var prev: PacketCacheCell? = null

    private var freshness: Instant? = null
    private var updateToken: Long? = null
    private var hidden: Boolean = false

    private var packetsByStation = HashMap<Ax25Address, LatLng>()

    private val parser = AprsParser()

    @Synchronized
    fun getUpdateUrl(): String? {
        val freshnessSnapshot = freshness
        this.freshness = Instant.now()
        if (freshnessSnapshot == null) {
            return "https://api.aprs.tools/within/$cell"
        }
        if (freshnessSnapshot.isAfter(Instant.now().minus(Duration.ofMinutes(2)))) {
            return null
        }
        if (freshnessSnapshot.isBefore(Instant.now().minus(Duration.ofHours(6)))) {
            return "https://api.aprs.tools/within/$cell"
        }
        return "https://api.aprs.tools/withinSince/$cell/${updateToken ?: 0}"
    }

    @Synchronized
    fun setHidden(newHidden: Boolean, plotter: PacketPlotter) {
        if (hidden == newHidden) {
            return
        }
        hidden = newHidden
        if (hidden) {
            plotter.hideAll(packetsByStation.keys.toList())
        } else {
            plotter.showAll(packetsByStation.keys.toList())
        }
    }

    @Synchronized
    fun update(command: CacheUpdateCommand, plotter: PacketPlotter) {
        if (command.evictAllOldStations) {
            plotter.removeAll(packetsByStation.keys.toList())
            packetsByStation.clear()
        }

        if (command.stationsToEvict.isNotEmpty()) {
            plotter.removeAll(command.stationsToEvict)
        }
        command.stationsToEvict.forEach {
            packetsByStation.remove(it)
        }

        val newPackets = command.newOrUpdated.map { timestampedSerializedPacket ->
            parser.parse(timestampedSerializedPacket.packet)?.let { TimestampedPacket(
                    timestampedSerializedPacket.millisSinceEpoch, it) }
        }.filterNotNull()

        newPackets.forEach {
            val aprsLatLng = it.packet.location() ?: return@forEach
            packetsByStation[it.packet.source] = LatLng(aprsLatLng.latitude, aprsLatLng.longitude)
        }
        if (!hidden) {
            plotter.plotOrUpdate(newPackets.map { it.packet })
        }

        updateToken = command.secondsSinceEpoch
    }

    @Synchronized
    fun getAllStations(): List<Ax25Address> {
        return packetsByStation.keys.toList()
    }
}