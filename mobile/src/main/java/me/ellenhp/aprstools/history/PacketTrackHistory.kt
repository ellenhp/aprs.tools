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

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.google.common.collect.ImmutableList
import com.google.common.collect.Multiset
import com.google.common.collect.SortedMultiset
import com.google.common.collect.TreeMultiset
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.packet.AprsTimestamp
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprstools.Utils
import java.time.Duration
import java.time.Instant
import java.time.temporal.TemporalAmount
import java.util.*
import javax.inject.Inject
import javax.inject.Provider
import kotlin.collections.HashMap

open class PacketTrackHistory() : Parcelable {

    var listener: HistoryUpdateListener? = null

    private val history: HashMap<Ax25Address, PacketTrack> = HashMap()

    constructor(parcel: Parcel) : this() {
        history.clear()
        while (true) {
            val station: Ax25Address = parcel.readParcelable(Ax25Address::class.java.classLoader) ?: return
            val track: PacketTrack = parcel.readParcelable(PacketTrack::class.java.classLoader) ?: return
            history[station] = track
        }
    }

    @Synchronized
    fun add(packet: AprsPacket, timestamp: Instant) {
        getOrCreateTrack(packet.source).addPacket(TimestampedPacket(packet, timestamp))
        listener?.historyUpate(packet.source)
    }

    @Synchronized
    fun getTrack(station: Ax25Address): ImmutableList<TimestampedPacket>? {
        return history.get(station)?.toImmutableList()
    }

    @Synchronized
    fun getStations(): ImmutableList<Ax25Address> {
        return ImmutableList.copyOf(history.keys)
    }

    private fun getOrCreateTrack(sourceStation: Ax25Address): PacketTrack {
        if (!history.containsKey(sourceStation)) {
            history.put(sourceStation, PacketTrack(sourceStation))
        }
        return history[sourceStation]!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        for (stationTrackPair in history) {
            parcel.writeParcelable(stationTrackPair.key, flags)
            parcel.writeParcelable(stationTrackPair.value, flags)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PacketTrackHistory> {
        override fun createFromParcel(parcel: Parcel): PacketTrackHistory {
            return PacketTrackHistory(parcel)
        }

        override fun newArray(size: Int): Array<PacketTrackHistory?> {
            return arrayOfNulls(size)
        }
    }
}

data class PacketTrack(val station: Ax25Address): Parcelable {
    private var packets = TreeMultiset.create<TimestampedPacket>()

    constructor(parcel: Parcel) : this(parcel.readParcelable<Ax25Address>(Ax25Address::class.java.classLoader)!!) {
        val packetArray = parcel.readArray(TimestampedPacket::class.java.classLoader)!!
        for (packet in packetArray) {
            packets.add(packet as TimestampedPacket)
        }
    }

    fun addPacket(timestampedPacket: TimestampedPacket) {
        packets.add(timestampedPacket)
    }

    fun toImmutableList(): ImmutableList<TimestampedPacket> {
        return packets.stream().collect(Utils.toImmutableList())
    }

    fun prune(currentTime: Instant) {
        val oldestAllowedTimestamp = currentTime.minus(Duration.ofDays(1))
        packets.removeIf{it.time.isBefore(oldestAllowedTimestamp)}
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(station, flags)
        parcel.writeArray(packets.toArray())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PacketTrack> {
        override fun createFromParcel(parcel: Parcel): PacketTrack {
            return PacketTrack(parcel)
        }

        override fun newArray(size: Int): Array<PacketTrack?> {
            return arrayOfNulls(size)
        }
    }
}

data class TimestampedPacket(val packet: AprsPacket, val time: Instant): Parcelable, Comparable<TimestampedPacket> {
    override fun compareTo(other: TimestampedPacket): Int {
        if (time.isBefore(other.time)) {
            return -1
        } else if (time.isAfter(other.time)) {
            return 1
        } else {
            return 0
        }
    }

    constructor(parcel: Parcel) : this(
            parcel.readParcelable(AprsPacket::class.java.classLoader)!!,
            Instant.parse(parcel.readString())) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(packet, flags)
        parcel.writeString(time.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TimestampedPacket> {
        override fun createFromParcel(parcel: Parcel): TimestampedPacket {
            return TimestampedPacket(parcel)
        }

        override fun newArray(size: Int): Array<TimestampedPacket?> {
            return arrayOfNulls(size)
        }
    }
}