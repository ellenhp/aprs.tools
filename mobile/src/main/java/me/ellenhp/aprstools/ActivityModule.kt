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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.google.android.gms.maps.GoogleMap
import dagger.Module
import dagger.Provides
import me.ellenhp.aprstools.tnc.TncDevice

@Module
class ActivityModule(private val activity: MainActivity) {

    @Provides
    fun provideMap(): GoogleMap? {
        return activity.map
    }

    @Provides
    fun providesBluetoothAdapter(): BluetoothAdapter? {
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    @Provides
    fun providesCreds(): UserCreds? {
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        val call = prefs.getString(PreferenceKeys.CALLSIGN, null) ?: return null
        val passcode = prefs.getString(PreferenceKeys.PASSCODE, null)
        return UserCreds(call, passcode)
    }

    @Provides
    fun providesTncDevice(bluetoothAdapter: BluetoothAdapter?): TncDevice? {
        bluetoothAdapter ?: return null // Can't do much here without a bluetooth adapter
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        val address = prefs.getString(PreferenceKeys.TNC_BT_ADDRESS, null) ?: return null
        return TncDevice(bluetoothAdapter.getRemoteDevice(address))
    }

    @Provides
    fun providesAprsIsServerAddress(): AprsIsServerAddress {
        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
        val host = prefs.getString(PreferenceKeys.APRS_IS_HOST, activity.getString(R.string.default_aprs_server))
        val port = prefs.getInt(PreferenceKeys.APRS_IS_PORT, activity.resources.getInteger(R.integer.default_aprs_port))
        return AprsIsServerAddress(host!!, port);
    }
}
