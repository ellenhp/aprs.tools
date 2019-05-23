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

import com.google.openlocationcode.OpenLocationCode
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprslib.packet.CacheUpdateCommandPosits
import me.ellenhp.aprstools.map.wrapper.MarkerDescriptor
import org.threeten.bp.Duration.ofMinutes
import org.threeten.bp.Instant

class PacketCacheCell(val code: OpenLocationCode) {

    private var freshness: Instant? = null
    private var plottedStations = HashSet<Ax25Address>()

    @Synchronized
    fun update(command: CacheUpdateCommandPosits, listener: CacheUpdateListener) {
        this.freshness = Instant.now()

        if (command.stationsToEvict.isNotEmpty()) {
            listener.evictStations(command.stationsToEvict)
            command.stationsToEvict.forEach {
                plottedStations.remove(it)
            }
        }

        if (command.evictAllOldStations) {
            val oldStations = plottedStations.minus(command.newOrUpdated.map { it.station })
            listener.evictStations(oldStations)
            plottedStations.clear()
        }

        command.newOrUpdated.forEach {
            plottedStations.add(it.station)
        }

        val markerDescriptors = command.newOrUpdated.map {
            val location = it.location.decode()
            MarkerDescriptor(it.station, location.centerLatitude, location.centerLongitude, it.symbol, Instant.ofEpochMilli(it.millisSinceEpoch))
        }
        listener.updateMarkers(markerDescriptors)
    }

    @Synchronized
    fun getAllStations(): List<Ax25Address> {
        return plottedStations.toList()
    }

    @Synchronized
    fun wantsUpdate(): Boolean {
        val freshnessSnapshot = freshness ?: return true
        // We want an update if our last update was before two minutes ago.
        return freshnessSnapshot.isBefore(Instant.now().minus(ofMinutes(2)))
    }
}