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

import com.google.android.gms.maps.model.LatLng
import com.google.common.truth.Truth.assertThat
import me.ellenhp.aprstools.aprs.testing.FakeSocketFactory
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.Socket
import me.ellenhp.aprslib.packet.*


class AprsIsClientTest {

    @Mock
    lateinit var socket: Socket

    lateinit var client: AprsIsClient

    val locationFilter = LocationFilter(LatLng(123.45, 67.89), 11.1)
    val examplePacket = "DW0398>APRS,TCPXX*,qAX,CWOP-5:@040628z4048.68N/08000.15W_140/000g000t040r000p001P000b10175h89.WD 31\r\n"

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        client = AprsIsClient(
                FakeSocketFactory(socket),
                "hostname",
                123,
                "KI7UKU",
                "123",
                locationFilter)
    }

    @Test
    fun testInit() {
        val input = "#hello\r\n#logged in\r\n"
        val inputStream = ByteArrayInputStream(input.toByteArray(Charsets.ISO_8859_1))
        val outputStream = ByteArrayOutputStream()

        Mockito.`when`(socket.getInputStream()).thenReturn(inputStream)
        Mockito.`when`(socket.getOutputStream()).thenReturn(outputStream)

        assertThat(client.readPacket()).isNull()
        assertThat(String(outputStream.toByteArray(), Charsets.ISO_8859_1)).isEqualTo(
                "user KI7UKU pass 123\r\n" +
                        "#filter default\r\n" +
                        "#filter r/90.000000/67.890000/11.100000\r\n")
    }

    @Test
    fun testReadPacket_parsesCorrectly() {
        val input = "#hello\r\n#logged in\r\n" + examplePacket
        val inputStream = ByteArrayInputStream(input.toByteArray(Charsets.ISO_8859_1))
        val outputStream = ByteArrayOutputStream()

        Mockito.`when`(socket.getInputStream()).thenReturn(inputStream)
        Mockito.`when`(socket.getOutputStream()).thenReturn(outputStream)

        assertThat(client.readPacket()).isNull() // #hello
        assertThat(client.readPacket()).isNull() // #logged in
        val packet: AprsPacket? = client.readPacket()

        assertThat(packet).isNotNull()
        assertThat(packet!!.source.call).isEqualTo("DW0398")
        assertThat(packet.dest.call).isEqualTo("APRS")
        assertThat(packet.path.pathSegments).containsExactly(
                PathSegment(Ax25Address("TCPXX", null), true),
                PathSegment(Ax25Address("qAX", null), false),
                PathSegment(Ax25Address("CWOP", "5"), false)
        )
        assertThat(packet.informationField.aprsData).isNotNull()
        assertThat(packet.informationField.aprsData.data).containsExactly(
                AprsTimestampDhm(4, 6, 28, AprsTimezone.ZULU),
                AprsPosition(
                        AprsLatLng(40.81133333333333, -80.0025, AprsPositAmbiguity.NEAREST_19_METERS),
                        AprsSymbol('/', '_')
                )
        )
    }

    @Test
    fun testWritePacket() {
        val input = "#hello\r\n#logged in\r\n"
        val inputStream = ByteArrayInputStream(input.toByteArray(Charsets.ISO_8859_1))
        val outputStream = ByteArrayOutputStream()

        Mockito.`when`(socket.getInputStream()).thenReturn(inputStream)
        Mockito.`when`(socket.getOutputStream()).thenReturn(outputStream)

        client.writePacket(AprsPacket(
                Ax25Address("KI7UKU", null),
                Ax25Address("APRS", null),
                AprsPath.directToAprsIs(),
                AprsInformationField.locationUpdate(123.45, 67.89)))

        assertThat(String(outputStream.toByteArray(), Charsets.ISO_8859_1)).isEqualTo(
                "user KI7UKU pass 123\r\n" +
                        "#filter default\r\n" +
                        "#filter r/90.000000/67.890000/11.100000\r\n" +
                        "KI7UKU>APRS,TCPIP*:=12327.00N/06753.40E\$Sent with APRSTools\r\n")
    }
}