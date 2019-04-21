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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UtilsTest {

    @Test
    fun testHaversineDistanceMeters_smallDistance() {
        val pos1 = LatLng(47.6253, 122.3222)
        val pos2 = LatLng(47.6256, 122.3344)
        assertThat(Utils.distanceMeters(pos1, pos2))
                .isWithin(1.0)
                .of(915.0)
    }

    @Test
    fun testHaversineDistanceMeters_largeDistance() {
        val pos1 = LatLng(47.6253, 122.3222)
        val pos2 = LatLng(37.7609, 122.4350)
        assertThat(Utils.distanceMeters(pos1, pos2))
                .isWithin(1.0)
                .of(1096911.0)
    }
}