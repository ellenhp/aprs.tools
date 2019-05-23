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

package me.ellenhp.aprstools.map.wrapper

import androidx.fragment.app.Fragment
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprstools.history.CacheUpdateListener

interface MapWrapper : CacheUpdateListener {
    /** Perform any initialization required by the map. */
    fun init(transitionToMapFragment: (Fragment) -> Unit)
    /** Draw the marker described by the provided descriptor. */
    fun drawOrUpdateMarker(markerDescriptor: MarkerDescriptor)
    /** Remove the marker for the specified station */
    fun removeMarker(ax25Address: Ax25Address)
    /** Returns whether or not the map is ready to have markers added to it */
    fun isReady(): Boolean
}