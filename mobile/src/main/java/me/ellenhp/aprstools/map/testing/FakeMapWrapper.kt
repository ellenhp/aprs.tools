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

package me.ellenhp.aprstools.map.testing

import android.content.Context
import androidx.fragment.app.Fragment
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprstools.ApplicationScope
import me.ellenhp.aprstools.map.wrapper.MapWrapper
import me.ellenhp.aprstools.map.wrapper.MarkerDescriptor
import javax.inject.Inject

class FakeMapWrapper @Inject constructor(@ApplicationScope context: Context) : MapWrapper {
    override fun init(transitionToMapFragment: (Fragment) -> Unit) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun drawOrUpdateMarker(markerDescriptor: MarkerDescriptor) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun removeMarker(ax25Address: Ax25Address) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun isReady(): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun updateMarkers(descriptors: Collection<MarkerDescriptor>) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun evictStations(stations: Collection<Ax25Address>) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}