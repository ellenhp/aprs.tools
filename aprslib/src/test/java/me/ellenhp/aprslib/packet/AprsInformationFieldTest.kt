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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AprsInformationFieldTest {

    @Test
    fun testLocationUpdate() {
        val infoField = AprsInformationField.locationUpdate(123.45678, 56.78)
        assertThat(infoField.toString()).isEqualTo("=12327.41N/05646.80E\$Sent with APRSTools")
    }
}