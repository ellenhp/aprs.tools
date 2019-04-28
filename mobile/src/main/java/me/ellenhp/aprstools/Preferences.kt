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

class PreferenceKeys {
    companion object {
        const val CALLSIGN = "callsign"
        const val SSID = "ssid"
        const val PASSCODE = "aprs_is_passcode"
        const val APRS_IS_HOST = "aprs_is_host"
        const val APRS_IS_PORT = "aprs_is_port"
    }
}

data class UserCreds(val call: String, val passcode: String?)
data class AprsIsServerAddress(val host: String, val port: Int)