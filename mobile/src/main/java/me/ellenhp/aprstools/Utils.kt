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

import com.google.android.gms.maps.model.LatLng
import java.lang.Math.toRadians
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class Utils {
    companion object {
        fun distanceMeters(pos1: LatLng, pos2: LatLng): Double {
            // Mean radius of the earth
            val radiusMeters = 6_371_008.8
            val distMeters = 2 * radiusMeters *
                asin(sqrt(
                    sin((toRadians(pos2.latitude) - toRadians(pos1.latitude)) / 2).pow(2.0) +
                    cos(toRadians(pos1.latitude)) * cos(toRadians(pos2.latitude)) *
                        sin((toRadians(pos2.longitude) - toRadians(pos1.longitude)) / 2).pow(2.0)
                ))
            return distMeters
        }
    }
}