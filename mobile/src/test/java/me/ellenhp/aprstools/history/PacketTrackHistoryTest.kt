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

import android.os.Parcel
import android.support.test.runner.AndroidJUnit4
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import me.ellenhp.aprslib.packet.AprsInformationField
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.packet.AprsPath
import me.ellenhp.aprslib.packet.Ax25Address
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PacketTrackHistoryTest {

    val packetTrackHistory = PacketTrackHistory()

    val exampleStation = Ax25Address("KI7UKU", null)
    val examplePacket1 = AprsPacket(
            exampleStation,
            Ax25Address("APRS", null),
            AprsPath(listOf()),
            AprsInformationField.locationUpdate(123.0, 45.0))
    val examplePacket2 = AprsPacket(
            exampleStation,
            Ax25Address("APRS", null),
            AprsPath(listOf()),
            AprsInformationField.locationUpdate(123.45678, 56.78))
    val startTime = Instant.ofEpochSecond(1555635010)

    @Test
    fun testGetTrack_simpleRecall() {
        packetTrackHistory.add(examplePacket1, startTime)
        packetTrackHistory.add(examplePacket2, startTime.plus(Duration.ofMinutes(1)))
        assertThat(packetTrackHistory.getTrack(exampleStation)).isEqualTo(
                ImmutableList.of(
                        TimestampedPacket(examplePacket1, startTime),
                        TimestampedPacket(examplePacket2, startTime.plus(Duration.ofMinutes(1)))))
    }

    @Test
    fun testGetTrack_sortedRecall() {
        packetTrackHistory.add(examplePacket2, startTime.plus(Duration.ofMinutes(1)))
        packetTrackHistory.add(examplePacket1, startTime)
        assertThat(packetTrackHistory.getTrack(exampleStation)).isEqualTo(
                ImmutableList.of(
                        TimestampedPacket(examplePacket1, startTime),
                        TimestampedPacket(examplePacket2, startTime.plus(Duration.ofMinutes(1)))))
    }

    @Test
    fun testGetTrack_groupsByStation() {
        val oddballStation = Ax25Address("KI7UKU", "HiMom")
        val oddballPacket = AprsPacket(
                oddballStation,
                Ax25Address("APRS", null),
                AprsPath(listOf()),
                AprsInformationField.locationUpdate(123.45678, 56.78))

        packetTrackHistory.add(examplePacket1, startTime)
        packetTrackHistory.add(examplePacket2, startTime.plus(Duration.ofMinutes(1)))
        packetTrackHistory.add(oddballPacket, startTime)
        assertThat(packetTrackHistory.getTrack(exampleStation)).isEqualTo(
                ImmutableList.of(
                        TimestampedPacket(examplePacket1, startTime),
                        TimestampedPacket(examplePacket2, startTime.plus(Duration.ofMinutes(1)))))
        assertThat(packetTrackHistory.getTrack(oddballStation)).isEqualTo(
                ImmutableList.of(
                        TimestampedPacket(oddballPacket, startTime)))
    }
}