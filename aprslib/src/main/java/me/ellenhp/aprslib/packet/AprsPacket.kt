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

import com.google.openlocationcode.OpenLocationCode

data class AprsPacket(val source: Ax25Address, val dest: Ax25Address, val path: AprsPath, val informationField: AprsInformationField) {
    fun isWeather(): Boolean {
        val standaloneWx = informationField.dataType in listOf('!', '#', '$', '*', '_')

        val symbol = informationField.aprsData.findDatumOfType<AprsPosition>()?.symbol
        val wxSymbol = symbol?.symbolTable in listOf('/', '\\') && symbol?.symbol == '_'

        return standaloneWx || wxSymbol
    }

    fun location(): AprsLatLng? {
        return informationField.aprsData.findDatumOfType<AprsPosition>()?.position
    }

    fun symbol(): AprsSymbol? {
        return informationField.aprsData.findDatumOfType<AprsPosition>()?.symbol
    }

    override fun toString(): String {
        return "%s>%s%s:%s".format(source, dest, path, informationField)
    }
}

data class CacheUpdateCommand(val evictAllOldStations: Boolean,
                              val secondsSinceEpoch: Long,
                              val newOrUpdated: List<TimestampedSerializedPacket>,
                              val stationsToEvict: List<Ax25Address>)
data class CacheUpdateCommandPosits(val evictAllOldStations: Boolean,
                                    val secondsSinceEpoch: Long,
                                    val newOrUpdated: List<TimestampedPosit>,
                                    val stationsToEvict: List<Ax25Address>)

data class TimestampedPacket(val millisSinceEpoch: Long,
                             val packet: AprsPacket)

data class TimestampedSerializedPacket(val millisSinceEpoch: Long,
                                       val packet: String)

data class TimestampedPosit(val millisSinceEpoch: Long,
                            val station: Ax25Address,
                            val location: OpenLocationCode)