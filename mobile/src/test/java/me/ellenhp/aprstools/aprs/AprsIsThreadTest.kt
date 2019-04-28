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

package me.ellenhp.aprstools.aprs

import me.ellenhp.aprslib.packet.AprsInformationField
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.packet.AprsPath
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprstools.Sleeper
import me.ellenhp.aprstools.history.PacketTrackHistory
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.threeten.bp.Instant
import javax.inject.Provider

class AprsIsThreadTest {

    var time = Instant.ofEpochSecond(1555973346)

    @Mock
    lateinit var history: PacketTrackHistory
    @Mock
    lateinit var client1: AprsIsClient
    @Mock
    lateinit var client2: AprsIsClient
    @Mock
    lateinit var sleeper: Sleeper

    lateinit var thread: AprsIsThread


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        thread = AprsIsThread(history, Provider { time }, sleeper)
    }

    @Test
    fun testSetClient_disconnects() {
        thread.setClient(client1)
        thread.setClient(client2)
        verify(client1).disconnect()
    }

    @Test(timeout = 100)
    fun testRun_exitsWhenInterruped() {
        var iterationCount = 0
        Mockito.`when`(client1.readPacket()).thenReturn(null)
        Mockito.`when`(sleeper.sleep(anyLong())).thenAnswer{
            iterationCount++
            if (iterationCount == 2) {
                Mockito.`when`(sleeper.sleep(anyLong())).thenAnswer{ throw InterruptedException() }
            }
        }
        thread.setClient(client1)
        thread.run()
        verify(client1, times(3)).readPacket()
        verifyNoMoreInteractions(client1)
    }

    @Test(timeout = 100)
    fun testRun_sendsQueuedPackets() {
        val packet = AprsPacket(
                Ax25Address("KI7UKU", null),
                Ax25Address("APRS", null),
                AprsPath.directToAprsIs(),
                AprsInformationField.locationUpdate(123.45, 67.89))
        var iterationCount = 0
        Mockito.`when`(client1.readPacket()).thenReturn(null)
        Mockito.`when`(sleeper.sleep(anyLong())).thenAnswer{
            iterationCount++
            if (iterationCount == 1) {
                thread.enqueuePacket(packet)
            }
            if (iterationCount == 2) {
                Mockito.`when`(sleeper.sleep(anyLong())).thenAnswer{ throw InterruptedException() }
            }
        }
        thread.setClient(client1)
        thread.run()
        verify(client1, times(3)).readPacket()
        verify(client1).writePacket(packet)
        verifyNoMoreInteractions(client1)
    }
}