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

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import me.ellenhp.aprslib.packet.AprsLatLng
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.packet.AprsSymbol
import me.ellenhp.aprslib.packet.Ax25Address

class PacketPlotter(private val activity: FragmentActivity, private val map: GoogleMap) {

    val markers = HashMap<Ax25Address, Marker>()
    val symbolTable = AprsSymbolTable(activity)

    @Synchronized
    fun removeAll(stationsToEvict: List<Ax25Address>) {
        activity.runOnUiThread {
            for (station in stationsToEvict) {
                markers[station]?.remove()
                markers.remove(station)
            }
        }
    }

    @Synchronized
    fun plotOrUpdate(packet: AprsPacket) {
        activity.runOnUiThread {
            Log.d("Plotter", "Plotting packet $packet")
            createOrUpdateMarker(packet)
        }
    }

    private fun createOrUpdateMarker(packet: AprsPacket) {
        val currentMarker = markers[packet.source]
        val location = packet.location() ?: return
        val symbol = packet.symbol() ?: return
        if (currentMarker == null) {
            createMarker(location, packet.source, symbol)?.let { markers[packet.source] = it }
        }
        else {
            currentMarker.position = LatLng(location.latitude, location.longitude)
        }
    }

    private fun createMarker(point: AprsLatLng, station: Ax25Address, symbol: AprsSymbol): Marker? {
        val markerOptions = MarkerOptions()
        val symbolDescriptor = symbolTable.getSymbol(symbol.symbolTable, symbol.symbol) ?: return null
        markerOptions.icon(symbolDescriptor)
        markerOptions.position(LatLng(point.latitude, point.longitude))
        markerOptions.anchor(0.5f, 0.5f)
        markerOptions.title(station.toString())
        return map.addMarker(markerOptions)
    }

}