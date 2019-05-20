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

import android.util.Log
import com.google.openlocationcode.OpenLocationCode

class ZoneUtils {

    fun getZonesWithin(southWestExact: OpenLocationCode.CodeArea, northEastExact: OpenLocationCode.CodeArea): List<OpenLocationCode> {
        // This looks silly, but giving the client a reasonable cache key is important for saving
        // bandwidth. Plus codes are compact and standardized.
        val southWest = OpenLocationCode(southWestExact.centerLatitude, southWestExact.centerLongitude, 4).decode()
        val northEast = OpenLocationCode(northEastExact.centerLatitude, northEastExact.centerLongitude, 4).decode()

        if (southWest.centerLongitude > northEast.centerLongitude) {
            error("southwest should not be east of northeast!")
        }
        if (southWest.centerLatitude > northEast.centerLatitude) {
            error("southwest should not be north of northeast!")
        }

        val zones = ArrayList<OpenLocationCode>()
        var lat = southWest.centerLatitude

        // Using the north latitude as an upper bound to avoid floating point errors.
        while (lat < northEast.northLatitude) {
            var long = southWest.centerLongitude
            while (long < northEast.eastLongitude) {
                zones.add(OpenLocationCode(lat, long, 4))
                long += southWest.longitudeWidth
            }
            lat += southWest.latitudeHeight
        }
        Log.d("zones", "returing ${zones.size}")
        return zones.toList()
    }
}