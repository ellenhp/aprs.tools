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

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.AppCompatButton
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprstools.aprs.AprsIsListener
import me.ellenhp.aprstools.aprs.AprsIsService
import me.ellenhp.aprstools.aprs.LocationFilter
import me.ellenhp.aprstools.modules.ActivityModule
import me.ellenhp.aprstools.settings.BluetoothPromptFragment
import me.ellenhp.aprstools.settings.CallsignDialogFragment
import me.ellenhp.aprstools.settings.PasscodeDialogFragment
import me.ellenhp.aprstools.tnc.TncDevice
import me.ellenhp.aprstools.tracker.TrackerService
import javax.inject.Inject
import javax.inject.Provider

class MainActivity : FragmentActivity(), OnMapReadyCallback, AprsIsListener, CoroutineScope by MainScope() {

    @Inject
    lateinit var fusedLocationClient: Lazy<FusedLocationProviderClient>
    @Inject
    lateinit var bluetoothAdapter: Lazy<BluetoothAdapter?>
    @Inject
    lateinit var userCreds: Provider<UserCreds?>
    @Inject
    lateinit var tncDevice: Provider<TncDevice?>

    val bluetoothDialog = BluetoothPromptFragment()
    val callsignDialog = CallsignDialogFragment()
    val passcodeDialog = PasscodeDialogFragment()

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            aprsIsService = (service as AprsIsService.AprsIsServiceBinder).getService()
            aprsIsService?.listener = this@MainActivity
        }

        override fun onServiceDisconnected(className: ComponentName) {
            aprsIsService = null
        }
    }

    var map: GoogleMap? = null
    var aprsIsService: AprsIsService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityCompoment = (application as AprsToolsApplication).component
                .newActivityComponent(ActivityModule(this))
        (application as AprsToolsApplication).activityComponent = activityCompoment
        activityCompoment.inject(this)

        setContentView(R.layout.activity_main)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // Start our AprsIsService.
        val intent = Intent(this, AprsIsService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()

        findViewById<AppCompatButton>(R.id.start_igate).setOnClickListener { startIGate() }
        findViewById<AppCompatButton>(R.id.start_tracking).setOnClickListener { startTracking() }

        launch {
            maybeShowCallsignDialog()
        }

        requestLocation()

        updateAprsIsListener()

        animateToLastLocation()
    }


    private fun startTracking() {
        launch {
            maybeShowCallsignDialog()
            maybeShowPasscodeDialog()
            runOnUiThread {
                // Start our AprsIsService.
                val intent = Intent(this@MainActivity, TrackerService::class.java)
                startService(intent)
            }
        }
    }

    private fun startIGate() {
        launch {
            maybeShowCallsignDialog()
            maybeShowBluetoothDialog()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val settings = googleMap.uiSettings

        settings.isCompassEnabled = true
        settings.isMyLocationButtonEnabled = true
        settings.isScrollGesturesEnabled = true
        settings.isZoomGesturesEnabled = true
        settings.isTiltGesturesEnabled = false
        settings.isRotateGesturesEnabled = true

        animateToLastLocation()
    }

    private suspend fun maybeShowCallsignDialog() {
        if (userCreds.get() == null) {
            callsignDialog.showBlocking(supportFragmentManager, "CallsignDialogFragment", this::runOnUiThread)

        }
    }

    private suspend fun maybeShowBluetoothDialog() {
        if (tncDevice.get() == null) {
            bluetoothDialog.showBlocking(supportFragmentManager, "BluetoothPromptFragment", this::runOnUiThread)
        }
    }

    private suspend fun maybeShowPasscodeDialog() {
        if (userCreds.get()?.passcode == null) {
            passcodeDialog.showBlocking(supportFragmentManager, "PasscodePromptFragment", this::runOnUiThread)
            aprsIsService?.resetClient()
        }
    }

    private fun requestLocation() {
        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
    }

    private fun animateToLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED)
            fusedLocationClient.get()?.lastLocation?.addOnSuccessListener(this) { this.processLocation(it) }
    }

    private fun updateAprsIsListener(location: Location? = null) {
        aprsIsService?.filter = LocationFilter(location ?: return, 50.0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != LOCATION_PERMISSION_REQUEST) {
            // TODO is this necessary?
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            map?.isMyLocationEnabled = true
            animateToLastLocation()
        }
    }

    override fun onAprsPacketReceived(packet: AprsPacket) {
        map ?: return

        val pos = packet.location() ?: return
        runOnUiThread {
            map?.addMarker(MarkerOptions()
                    .position(LatLng(pos.latitude, pos.longitude))
                    .title(packet.source.toString()))
        }
    }

    private fun processLocation(location: Location?) {
        location ?: return
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 10001
    }
}
