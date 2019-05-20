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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import me.ellenhp.aprstools.history.Posit

class PositRenderer(context: Context, val map: GoogleMap, clusterManager: ClusterManager<Posit>):
        DefaultClusterRenderer<Posit>(context, map, clusterManager) {

    override fun getMarker(clusterItem: Posit): Marker {
        val markerOptions = MarkerOptions()
        val symbolDescriptor = clusterItem.symbol
        markerOptions.icon(symbolDescriptor)
        markerOptions.position(clusterItem.posit)
        markerOptions.anchor(0.5f, 0.5f)
        markerOptions.title(clusterItem.title)
        markerOptions.snippet(clusterItem.snippet)
        return map.addMarker(markerOptions)
    }

    override fun onBeforeClusterItemRendered(item: Posit, markerOptions: MarkerOptions) {
        markerOptions.icon(item.symbol)
    }
}