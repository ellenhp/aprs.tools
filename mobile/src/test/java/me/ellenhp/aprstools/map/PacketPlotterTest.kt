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

package me.ellenhp.aprstools.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import com.google.common.collect.ImmutableList
import me.ellenhp.aprslib.packet.AprsInformationField
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.packet.AprsPath
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprstools.history.PacketTrackHistory
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import java.time.Duration
import java.time.Duration.ofHours
import java.time.Instant
import javax.inject.Provider
import dagger.Lazy
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PacketPlotterTest {

    @Mock
    lateinit var map: GoogleMap
    @Mock
    lateinit var polyline1: Polyline
    @Mock
    lateinit var polyline2: Polyline
    @Mock
    lateinit var marker1: Marker
    @Mock
    lateinit var symbolTable: AprsSymbolTable
    @Mock
    lateinit var symbolDescriptor: BitmapDescriptor

    lateinit var time: Instant
    lateinit var packetPlotter: PacketPlotter
    lateinit var history: PacketTrackHistory

    val packet1 = AprsPacket(
            Ax25Address("KI7UKU", null),
            Ax25Address("APRS", null),
            AprsPath.directToAprsIs(),
            AprsInformationField.locationUpdate(123.45, 67.89))

    val packet2 = AprsPacket(
            Ax25Address("KI7UKU", null),
            Ax25Address("APRS", null),
            AprsPath.directToAprsIs(),
            AprsInformationField.locationUpdate(123.46, 67.90))

    val packet3 = AprsPacket(
            Ax25Address("KI7UKU", null),
            Ax25Address("APRS", null),
            AprsPath.directToAprsIs(),
            AprsInformationField.locationUpdate(123.47, 67.91))

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        MapsInitializer.initialize(getApplicationContext())
        time = Instant.ofEpochSecond(1555635010)
        history = PacketTrackHistory()

        packetPlotter = PacketPlotter(Provider {time}, Lazy {symbolTable}, map, ofHours(6))

        Mockito.`when`(symbolTable.getSymbol(anyChar(), anyChar())).thenReturn(symbolDescriptor)
    }

    @Test
    fun testNoStations() {
        packetPlotter.plot(history)

        verifyNoMoreInteractions(map)
    }

    @Test
    fun testSingleMarker() {
        Mockito.`when`(map.addMarker(any())).thenReturn(marker1)
        history.add(packet1, time)
        passTime(ofHours(1))

        packetPlotter.plot(history)

        verify(map).addMarker(argThat{ it.position == LatLng(123.45, 67.89) })
        verifyNoMoreInteractions(map)
    }

    @Test
    fun testSingleMarker_noSymbol() {
        Mockito.`when`(symbolTable.getSymbol(anyChar(), anyChar())).thenReturn(null)
        Mockito.`when`(map.addMarker(any())).thenReturn(marker1)
        history.add(packet1, time)
        passTime(ofHours(1))

        packetPlotter.plot(history)

        verifyNoMoreInteractions(map)
    }

    @Test
    fun testSingleMarker_prunedWhenOld() {
        Mockito.`when`(map.addMarker(any())).thenReturn(marker1)
        history.add(packet1, time)
        passTime(ofHours(1))

        packetPlotter.plot(history)

        verify(map).addMarker(argThat{ it.position == LatLng(123.45, 67.89) })
        passTime(ofHours(7))

        packetPlotter.plot(history)
        verify(marker1).remove()
        verifyNoMoreInteractions(map)
    }

    @Test
    fun testSingleMarker_prunedThenRecreatedWhenOld() {
        Mockito.`when`(map.addMarker(any())).thenReturn(marker1)
        history.add(packet1, time)
        passTime(ofHours(1))

        packetPlotter.plot(history)

        verify(map).addMarker(argThat{ it.position == LatLng(123.45, 67.89) })

        passTime(ofHours(7))
        packetPlotter.plot(history)
        verify(marker1).remove()

        passTime(ofHours(1))
        history.add(packet2, time)
        packetPlotter.plot(history)

        verify(map).addMarker(argThat{ it.position == LatLng(123.46, 67.90) })
    }

    @Test
    fun testSingleMarker_moved() {
        Mockito.`when`(map.addPolyline(any())).thenReturn(polyline1)
        Mockito.`when`(map.addMarker(any())).thenReturn(marker1)
        history.add(packet1, time)
        passTime(ofHours(1))

        packetPlotter.plot(history)

        verify(map).addMarker(argThat{ it.position == LatLng(123.45, 67.89) })

        passTime(ofHours(1))
        history.add(packet2, time)
        packetPlotter.plot(history)

        verify(marker1).position = LatLng(123.46, 67.90)
    }

    @Test
    fun testSingleMarker_expired() {
        history.add(packet1, time)
        passTime(ofHours(7))

        packetPlotter.plot(history)

        verifyNoMoreInteractions(map)
    }

    @Test
    fun testSinglePolyline() {
        Mockito.`when`(map.addPolyline(any())).thenReturn(polyline1)
        Mockito.`when`(map.addMarker(any())).thenReturn(marker1)
        history.add(packet1, time)
        passTime(ofHours(1))
        history.add(packet2, time)

        packetPlotter.plot(history)

        verify(map).addPolyline(argThat{
            it.points == ImmutableList.of(LatLng(123.45, 67.89), LatLng(123.46, 67.90))
        })
    }

    @Test
    fun testSinglePolyline_prunedWhenOld() {
        Mockito.`when`(map.addPolyline(any())).thenReturn(polyline1)
        Mockito.`when`(map.addMarker(any())).thenReturn(marker1)
        history.add(packet1, time)
        passTime(ofHours(4))
        history.add(packet2, time)

        packetPlotter.plot(history)

        verify(map).addPolyline(argThat{
            it.points == ImmutableList.of(LatLng(123.45, 67.89), LatLng(123.46, 67.90))
        })
        verifyNoMoreInteractions(polyline1)
        verifyNoMoreInteractions(marker1)

        passTime(ofHours(4))

        packetPlotter.plot(history)

        verify(polyline1).remove()
    }

    @Test
    fun testSinglePolyline_prunedThenRecreatedWhenOld() {
        Mockito.`when`(map.addPolyline(any())).thenReturn(polyline1)
        Mockito.`when`(map.addMarker(any())).thenReturn(marker1)
        history.add(packet1, time)
        passTime(ofHours(4))
        history.add(packet2, time)

        packetPlotter.plot(history)

        verify(map).addPolyline(argThat{
            it.points == ImmutableList.of(LatLng(123.45, 67.89), LatLng(123.46, 67.90))
        })
        verifyNoMoreInteractions(polyline1)

        passTime(ofHours(4))

        packetPlotter.plot(history)

        verify(polyline1).remove()

        history.add(packet3, time)
        Mockito.`when`(map.addPolyline(any())).thenReturn(polyline2)

        packetPlotter.plot(history)

        verify(map).addPolyline(argThat{
            it.points == ImmutableList.of(LatLng(123.46, 67.90), LatLng(123.47, 67.91))
        })
    }

    private fun passTime(duration: Duration) {
        time = time.plus(duration)
    }
}