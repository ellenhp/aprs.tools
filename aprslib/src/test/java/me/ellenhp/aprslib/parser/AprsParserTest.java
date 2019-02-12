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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.ellenhp.aprslib.parser;

import org.junit.Test;

import me.ellenhp.aprslib.packet.AprsLatLng;
import me.ellenhp.aprslib.packet.AprsPacket;
import me.ellenhp.aprslib.packet.AprsPositAmbiguity;
import me.ellenhp.aprslib.packet.AprsPosition;
import me.ellenhp.aprslib.packet.AprsSymbol;
import me.ellenhp.aprslib.packet.AprsTimestampDhm;
import me.ellenhp.aprslib.packet.AprsTimezone;
import me.ellenhp.aprslib.packet.Ax25Address;
import me.ellenhp.aprslib.packet.PathSegment;

import static com.google.common.truth.Truth.assertThat;

/* APRS messages transmitted over the air are public domain like all amateur radio transmissions. */
public class AprsParserTest {

    private AprsParser parser = new AprsParser();

    @Test
    public void testParseWeatherMessage() {
        AprsPacket packet = parser.parse("DW0398>APRS,TCPXX*,qAX,CWOP-5:@040628z4048.68N/08000.15W_140/000g000t040r000p001P000b10175h89.WD 31");
        assertThat(packet).isNotNull();
        assertThat(packet.getSource().getCall()).isEqualTo("DW0398");
        assertThat(packet.getDest().getCall()).isEqualTo("APRS");
        assertThat(packet.getPath().getPathSegments()).containsExactly(
                new PathSegment(new Ax25Address("TCPXX", null), true),
                new PathSegment(new Ax25Address("qAX", null), false),
                new PathSegment(new Ax25Address("CWOP", "5"), false)
        );
        assertThat(packet.getInformationField().getAprsData()).isNotNull();
        assertThat(packet.getInformationField().getAprsData().getData()).containsExactly(
                new AprsTimestampDhm(4, 6, 28, AprsTimezone.ZULU),
                new AprsPosition(
                        new AprsLatLng(40.81133333333333, -80.0025, AprsPositAmbiguity.NEAREST_19_METERS),
                        new AprsSymbol('/', '_')
                )
        );
        // TODO: Parse weather info.
        assertThat(packet.isWeather()).isTrue();
    }

    @Test
    public void testParseIsReversible() {
        String rawPacket = "DW0398>APRS,TCPXX*,qAX,CWOP-5:@040628z4048.68N/08000.15W_140/000g000t040r000p001P000b10175h89.WD 31";
        AprsPacket packet = parser.parse(rawPacket);
        assertThat(packet).isNotNull();
        assertThat(packet.toString()).isEqualTo(rawPacket);
    }
}
