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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprstools.aprs.AprsIsListener
import me.ellenhp.aprstools.aprs.AprsIsService
import me.ellenhp.aprstools.aprs.LocationFilter

class MapActivity : FragmentActivity(), OnMapReadyCallback, AprsIsListener {

    private val tnc: BluetoothDevice?
        get() {
            val address = getPreferences(Context.MODE_PRIVATE).getString(getString(R.string.TNC_BT_ADDRESS), null)
                    ?: return null
            return bluetoothAdapter?.getRemoteDevice(address)
        }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            aprsIsService = (service as AprsIsService.AprsIsServiceBinder).getService()
            aprsIsService?.listener = this@MapActivity
        }

        override fun onServiceDisconnected(className: ComponentName) {
            aprsIsService = null
        }
    }

    private var mMap: GoogleMap? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var aprsIsService: AprsIsService? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        findViewById<AppCompatButton>(R.id.start_igate).setOnClickListener { startIGate() }
        findViewById<AppCompatButton>(R.id.start_tracking).setOnClickListener { startTracking() }

        if (getCallsign() == null)
            showCallsignDialog()

        requestLocation()

        updateAprsIsListener()
    }


    private fun startTracking() {
        if (tnc == null) {
            showBluetoothDialog()
        }
    }

    private fun startIGate() {
        if (tnc == null) {
            showBluetoothDialog()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val settings = googleMap.uiSettings

        settings.isCompassEnabled = true
        settings.isMyLocationButtonEnabled = true
        settings.isScrollGesturesEnabled = true
        settings.isZoomGesturesEnabled = true
        settings.isTiltGesturesEnabled = false
        settings.isRotateGesturesEnabled = true

        animateToLastLocation()
    }

    private fun getCallsign(): String? {
        val call = getPreferences(Context.MODE_PRIVATE).getString(getString(R.string.callsign_pref), null)
        return call
    }

    private fun showCallsignDialog() {
        val dialogFragment = CallsignDialogFragment()
        dialogFragment.show(supportFragmentManager, "CallsignDialogFragment")
    }

    private fun showBluetoothDialog() {
        val dialogFragment = BluetoothPromptFragment()
        dialogFragment.show(supportFragmentManager, "BluetoothPromptFragment")
    }

    private fun requestLocation() {
        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
    }

    private fun animateToLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED)
            mFusedLocationClient?.lastLocation?.addOnSuccessListener(this) { this.processLocation(it) }
    }

    private fun updateAprsIsListener(location: Location? = null) {
        aprsIsService?.callsign = getPreferences(Context.MODE_PRIVATE).getString(getString(R.string.callsign_pref), null)
        aprsIsService?.callsign = getCallsign()
        aprsIsService?.host = getString(R.string.aprs_server)
        aprsIsService?.port = resources.getInteger(R.integer.aprs_port)
        aprsIsService?.filter = LocationFilter(location ?: return, 50.0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != LOCATION_PERMISSION_REQUEST) {
            // TODO is this necessary?
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            mMap?.isMyLocationEnabled = true
            animateToLastLocation()
        }
    }

    override fun onAprsPacketReceived(packet: AprsPacket) {
        mMap ?: return

        val pos = packet.location() ?: return
        runOnUiThread {addMarkerToMap(MarkerOptions()
                .position(LatLng(pos.latitude, pos.longitude))
                .title(packet.source.toString()))}
    }

    private fun addMarkerToMap(marker: MarkerOptions) {
        mMap?.addMarker(marker)
    }

    private fun processLocation(location: Location?) {
        location ?: return
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))

    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 10001
    }
}
