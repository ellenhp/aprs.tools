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

package me.ellenhp.aprstools.map

import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import me.ellenhp.aprslib.packet.*
import me.ellenhp.aprstools.history.Posit
import org.threeten.bp.Instant
import java.lang.Math.abs

class PacketPlotter(private val activity: FragmentActivity, private val map: GoogleMap) {

    val markers = HashMap<Ax25Address, Posit>()
    val symbolTable = AprsSymbolTable(activity)
    val clusterManager = ClusterManager<Posit>(activity, map)

    init {
        clusterManager.renderer = PositRenderer(activity, map, clusterManager)
        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);
    }

    @Synchronized
    fun removeAll(stationsToEvict: List<Ax25Address>) {
        activity.runOnUiThread {
            for (station in stationsToEvict) {
                markers[station]?.let { clusterManager.removeItem(it) }
                markers.remove(station)
            }
        }
    }

    @Synchronized
    fun plotOrUpdate(packets: List<TimestampedPosit>) {
        activity.runOnUiThread {
            packets.forEach {
                createOrUpdateMarker(it)
            }
            clusterManager.cluster()
        }
    }

    private fun createOrUpdateMarker(posit: TimestampedPosit) {
        val currentMarker = markers[posit.station]
        val location = LatLng(posit.location.decode().centerLatitude,
                posit.location.decode().centerLongitude)
        val symbol = posit.symbol
        if (currentMarker == null) {
            val marker = createMarker(location, posit.station, symbol,
                    Instant.ofEpochMilli(posit.millisSinceEpoch))
            marker?.let { markers[posit.station] = it }
            clusterManager.addItem(marker)
        }
        else {
            val newPos = LatLng(location.latitude, location.longitude)
            if (abs(currentMarker.position.latitude - newPos.latitude) > 0.0000001 ||
                    abs(currentMarker.position.longitude - newPos.longitude) > 0.0000001) {
                currentMarker.posit = newPos
            }
        }
    }

    private fun createMarker(point: LatLng,
                             station: Ax25Address,
                             symbol: AprsSymbol,
                             lastHeard: Instant): Posit? {
        val symbolDescriptor = symbolTable.getSymbol(symbol.symbolTable, symbol.symbol) ?: return null

        return Posit(station.toString(),
                point,
                symbolDescriptor,
                lastHeard,
                activity.resources.configuration.locale)
    }

}