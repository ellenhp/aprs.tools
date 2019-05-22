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

package me.ellenhp.aprstools.license

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import me.ellenhp.aprstools.licenses.Dependency
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE, sdk = [21, 23, 24, 26, 28])
class DependencyTest {

    lateinit var licenseYamlFile: String

    @Before
    fun setUp() {
        licenseYamlFile = ApplicationProvider.getApplicationContext<Context>()
                .assets.open("licenses.yml").reader().readText()
    }

    @Test
    fun testParseSucceeds() {
        val licenses = Dependency.parseFile(licenseYamlFile)
        assertThat(licenses).isNotNull()
        assertThat(licenses).isNotEmpty()
        print(licenses!!::class.java.canonicalName)
        print(licenses[0]::class.java.canonicalName)
        assertThat(licenses).contains(Dependency(
                "androidx.legacy:legacy-support-v4:+",
                "Android Support Library v4",
                "Copyright (c) 2005-2013, The Android Open Source Project",
                "The Apache Software License, Version 2.0",
                "http://www.apache.org/licenses/LICENSE-2.0.txt",
                "http://developer.android.com/tools/extras/support-library.html"))
    }
}