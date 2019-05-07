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

import com.google.common.collect.ImmutableList
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.packet.Ax25Address
import org.threeten.bp.Instant
import java.util.*
import kotlin.collections.HashMap

open class PacketTrackHistory {

    private val history: HashMap<Ax25Address, PacketTrack> = HashMap()

    @Synchronized
    fun add(packet: AprsPacket, timestamp: Instant) {
        getOrCreateTrack(packet.source).addPacket(TimestampedPacket(packet, timestamp))
    }

    @Synchronized
    fun getTrack(station: Ax25Address): ImmutableList<TimestampedPacket>? {
        return history[station]?.toImmutableList()
    }

    @Synchronized
    fun getStations(): ImmutableList<Ax25Address> {
        return ImmutableList.copyOf(history.keys)
    }

    private fun getOrCreateTrack(sourceStation: Ax25Address): PacketTrack {
        if (!history.containsKey(sourceStation)) {
            history[sourceStation] = PacketTrack(sourceStation)
        }
        return history[sourceStation]!!
    }
}

data class PacketTrack(val station: Ax25Address) {
    private var packets = TreeSet<TimestampedPacket>()

    fun addPacket(timestampedPacket: TimestampedPacket) {
        packets.add(timestampedPacket)
    }

    fun toImmutableList(): ImmutableList<TimestampedPacket> {
        return ImmutableList.copyOf(packets)
    }
}

data class TimestampedPacket(val packet: AprsPacket, val time: Instant): Comparable<TimestampedPacket> {
    override fun compareTo(other: TimestampedPacket): Int {
        if (time.isBefore(other.time)) {
            return -1
        } else if (time.isAfter(other.time)) {
            return 1
        } else {
            return 0
        }
    }
}