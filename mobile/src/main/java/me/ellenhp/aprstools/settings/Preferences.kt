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

package me.ellenhp.aprstools.settings

import android.content.SharedPreferences
import me.ellenhp.aprslib.packet.Ax25Address
import javax.inject.Inject

class PreferenceKeys {
    companion object {
        const val CALLSIGN = "callsign"
        const val SSID = "ssid"
        const val PASSCODE = "aprs_is_passcode"
        const val APRS_IS_HOST = "aprs_is_host"
        const val APRS_IS_PORT = "aprs_is_port"
    }
}

class Preferences @Inject constructor(private val sharedPreferences: SharedPreferences) {
    fun getAprsIsCredentials(): UserCredentials? {
        val call = sharedPreferences.getString(PreferenceKeys.CALLSIGN, null) ?: return null
        val passcode = sharedPreferences.getString(PreferenceKeys.PASSCODE, null)
        return UserCredentials(call, passcode)
    }

    fun getAprsOriginStation(): Ax25Address? {
        val call = sharedPreferences.getString(PreferenceKeys.CALLSIGN, null) ?: return null
        val ssid = sharedPreferences.getString(PreferenceKeys.SSID, null)
        return Ax25Address(call, ssid)
    }

    fun getAprsIsServerAddress(): AprsIsServerAddress? {
        val host = sharedPreferences.getString(PreferenceKeys.APRS_IS_HOST, null) ?: return null
        val port = sharedPreferences.getString(PreferenceKeys.APRS_IS_PORT, "0")?.toIntOrNull() ?: return null
        return AprsIsServerAddress(host, port)
    }

    fun getCallsign(): String? {
        return sharedPreferences.getString(PreferenceKeys.CALLSIGN, null)
    }
}

data class UserCredentials(val call: String, val passcode: String?)
data class AprsIsServerAddress(val host: String, val port: Int)