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

package me.ellenhp.aprstools.tracker

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import me.ellenhp.aprslib.packet.AprsInformationField
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprslib.packet.AprsPath
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprstools.AprsToolsApplication
import me.ellenhp.aprstools.UserCreds
import me.ellenhp.aprstools.aprs.AprsIsService
import javax.inject.Inject
import javax.inject.Provider

class TrackerService : Service() {

    @Inject
    lateinit var aprsIsService: Provider<AprsIsService>
    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject
    lateinit var userCreds: Provider<UserCreds?>

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        (application as AprsToolsApplication).activityComponent?.inject(this)

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(LocationRequest.create(), object : LocationCallback() {
                override fun onLocationResult(location: LocationResult?) {
                    onLocation(location ?: return)
                }
            }, Looper.getMainLooper())
        }

        return START_NOT_STICKY
    }

    fun onLocation(location: LocationResult) {
        val creds = userCreds.get() ?: return
        val originStation = Ax25Address(creds.call, /* TODO make SSID configurable */ "5")
        val destination = Ax25Address("APRS", null)
        val path = AprsPath.directToAprsIs()
        // TODO match accuracy with position ambiguity?
        val informationField = AprsInformationField.locationUpdate(location.lastLocation.latitude, location.lastLocation.longitude)
        val packet = AprsPacket(originStation, destination, path, informationField)
        aprsIsService.get().sendPacket(packet)
    }
}