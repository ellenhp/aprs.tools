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

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import com.google.maps.android.clustering.ClusterManager
import me.ellenhp.aprslib.packet.AprsSymbol
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprslib.packet.TimestampedPosit
import me.ellenhp.aprstools.MapFragmentScope
import me.ellenhp.aprstools.history.Posit
import org.jetbrains.anko.runOnUiThread
import org.threeten.bp.Instant
import java.lang.Math.abs
import javax.inject.Inject

@MapFragmentScope
@AutoFactory(allowSubclasses = true)
class PacketPlotter(@Provided private val context: Context, map: GoogleMap) : GoogleMap.OnCameraIdleListener {

    private val markers = HashMap<Ax25Address, Posit>()
    private val symbolTable = AprsSymbolTable(context)
    private val clusterManager = ClusterManager<Posit>(context, map)

    init {
        clusterManager.renderer = PositRenderer(context, map, clusterManager)
        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)
    }

    @Synchronized
    fun removeAll(stationsToEvict: List<Ax25Address>) {
        context.runOnUiThread {
            for (station in stationsToEvict) {
                markers[station]?.let { clusterManager.removeItem(it) }
                markers.remove(station)
            }
        }
    }

    @Synchronized
    fun plotOrUpdate(packets: List<TimestampedPosit>) {
        context.runOnUiThread {
            packets.forEach {
                createOrUpdateMarker(it)
            }
            clusterManager.cluster()
        }
    }

    override fun onCameraIdle() {
        clusterManager.onCameraIdle()
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
        } else {
            val newPos = LatLng(location.latitude, location.longitude)
            if (abs(currentMarker.position.latitude - newPos.latitude) > 0.0000001 ||
                    abs(currentMarker.position.longitude - newPos.longitude) > 0.0000001) {
                currentMarker.posit = newPos
            }
        }
    }

    private fun createMarker(
        point: LatLng,
        station: Ax25Address,
        symbol: AprsSymbol,
        lastHeard: Instant
    ): Posit? {
        val symbolDescriptor = symbolTable.getSymbol(symbol.symbolTable, symbol.symbol) ?: return null

        // Locale is deprecated but the recommended replacement doesn't work back to API 21.
        @Suppress("DEPRECATION")
        return Posit(station.toString(),
                point,
                symbolDescriptor,
                lastHeard,
                context.resources.configuration.locale)
    }
}