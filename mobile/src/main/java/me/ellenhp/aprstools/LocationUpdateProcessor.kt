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

package me.ellenhp.aprstools

import android.app.Activity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import me.ellenhp.aprslib.packet.AprsPacket
import javax.inject.Inject
import dagger.Lazy
import me.ellenhp.aprstools.aprs.AprsIsListener

class LocationUpdateProcessor : AprsIsListener {

    @Inject lateinit var map: Lazy<GoogleMap>
    @Inject lateinit var activity: Activity

    override fun onAprsPacketReceived(packet: AprsPacket) {
        val pos = packet.location() ?: return
        activity.runOnUiThread {map.get()?.addMarker(MarkerOptions()
                .position(LatLng(pos.latitude, pos.longitude))
                .title(packet.source.toString()))}
    }
}