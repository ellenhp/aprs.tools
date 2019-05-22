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

package me.ellenhp.aprslib.packet

import kotlin.math.abs
import kotlin.math.round
import kotlin.math.truncate

enum class AprsPositAmbiguity(val boundingCircleRadiusMeters: Int, val omittedSpaces: Int) {
    NEAREST_19_METERS(9, 0),
    NEAREST_185_METERS(93, 1),
    NEAREST_1852_METERS(926, 2),
    NEAREST_18522_METERS(9261, 3),
    NEAREST_185220_METERS(92610, 4);

    companion object {
        private val map = AprsPositAmbiguity.values().associateBy(AprsPositAmbiguity::omittedSpaces)
        fun fromOmmittedSpaces(spaces: Int) = map[spaces]
    }
}

data class AprsLatLng(val latitude: Double, val longitude: Double, val ambiguity: AprsPositAmbiguity)

data class AprsSymbol(val symbolTable: Char, val symbol: Char)

data class AprsPosition(val position: AprsLatLng, val symbol: AprsSymbol) : AprsDatum {

    override fun toString(): String {
        val lat = formatAngle("%02d%02d.%02d%s", position.latitude, "S", "N")
        val lng = formatAngle("%03d%02d.%02d%s", position.longitude, "W", "E")
        return "%s%c%s%c".format(lat, symbol.symbolTable, lng, symbol.symbol)
    }

    private fun formatAngle(format: String, angle: Double, negativeSign: String, positiveSign: String): String {
        val wholeDegrees = truncate(angle).toInt()
        val minutes = abs(angle - wholeDegrees) * 60
        val wholeMinutes = truncate(minutes).toInt()
        val hundredthsMinutes = round((minutes - wholeMinutes) * 100).toInt()
        return format.format(
                abs(wholeDegrees),
                wholeMinutes,
                hundredthsMinutes,
                if (angle < 0)
                    negativeSign
                else
                    positiveSign
        )
    }
}
