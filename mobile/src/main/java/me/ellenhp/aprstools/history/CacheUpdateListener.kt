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

import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprstools.map.wrapper.MarkerDescriptor

interface CacheUpdateListener {
    /** Update the specified markers on the map */
    fun updateMarkers(descriptors: Collection<MarkerDescriptor>)
    /** Remove any markers from the specified stations. */
    fun evictStations(stations: Collection<Ax25Address>)
}