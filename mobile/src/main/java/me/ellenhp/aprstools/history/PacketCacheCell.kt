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

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.openlocationcode.OpenLocationCode
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprslib.packet.CacheUpdateCommand
import me.ellenhp.aprslib.packet.CacheUpdateCommandPosits
import me.ellenhp.aprslib.packet.TimestampedPacket
import me.ellenhp.aprslib.parser.AprsParser
import me.ellenhp.aprstools.map.PacketPlotter
import org.threeten.bp.Duration
import org.threeten.bp.Instant

class PacketCacheCell(val cell: OpenLocationCode) {

    private var freshness: Instant? = null
    private var hidden: Boolean = false

    private var packetsByStation = HashMap<Ax25Address, LatLng>()

    private val parser = AprsParser()

    @Synchronized
    fun getUpdateUrl(): String? {
        val freshnessSnapshot = freshness
        this.freshness = Instant.now()
        if (freshnessSnapshot == null) {
            return "https://api.aprs.tools/within/$cell?type=posit"
        }
        if (freshnessSnapshot.isAfter(Instant.now().minus(Duration.ofMinutes(2)))) {
            return null
        }
        Log.d("update", "Updating cell")
        return "https://api.aprs.tools/within/$cell?type=posit"
    }

    @Synchronized
    fun update(command: CacheUpdateCommandPosits, plotter: PacketPlotter) {
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

        command.newOrUpdated.forEach {
            val location = it.location.decode()
            packetsByStation[it.station] = LatLng(location.centerLatitude, location.centerLongitude)
        }
        if (!hidden) {
            plotter.plotOrUpdate(command.newOrUpdated)
        }
    }

    @Synchronized
    fun getAllStations(): List<Ax25Address> {
        return packetsByStation.keys.toList()
    }

    @Synchronized
    fun resetFreshness() {
        freshness = null
    }
}