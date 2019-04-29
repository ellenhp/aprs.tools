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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.*
import com.google.android.material.navigation.NavigationView
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.ellenhp.aprstools.aprs.AprsIsService
import me.ellenhp.aprstools.aprs.LocationFilter
import me.ellenhp.aprstools.history.PacketTrackHistory
import me.ellenhp.aprstools.licenses.AprsToolsLicenseFragment
import me.ellenhp.aprstools.licenses.DependencyLicenseFragment
import me.ellenhp.aprstools.modules.ActivityModule
import me.ellenhp.aprstools.settings.CallsignDialogFragment
import me.ellenhp.aprstools.settings.Preferences
import javax.inject.Inject

class MainActivity : androidx.appcompat.app.AppCompatActivity(),
        MapViewFragment.OnFragmentInteractionListener,
        AboutFragment.OnFragmentInteractionListener,
        DependencyLicenseFragment.OnFragmentInteractionListener,
        AprsToolsLicenseFragment.OnFragmentInteractionListener,
        CoroutineScope by MainScope(), NavController.OnDestinationChangedListener {

    @Inject
    lateinit var fusedLocationClient: Lazy<FusedLocationProviderClient>
    @Inject
    lateinit var preferences: Lazy<Preferences>

    private val callsignDialog = CallsignDialogFragment()

    lateinit var packetHistory: PacketTrackHistory
    private val packetHistoryBundleKey = "PacketTrackHistory"

    var aprsIsService: AprsIsService? = null

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            aprsIsService = (service as AprsIsService.AprsIsServiceBinder).getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            aprsIsService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityComponent = (application as AprsToolsApplication).component
                .newActivityComponent(ActivityModule(this))
        (application as AprsToolsApplication).activityComponent = activityComponent
        activityComponent.inject(this)

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        findViewById<NavigationView>(R.id.nav_view).setNavigationItemSelectedListener(this::onOptionsItemSelected)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.isDrawerIndicatorEnabled = true
        toggle.syncState()

        val packetHistoryBundle = savedInstanceState?.getBundle(packetHistoryBundleKey)
        packetHistory = packetHistoryBundle?.getParcelable(packetHistoryBundleKey) ?: PacketTrackHistory()

        val navController = findNavController(supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!)
        navController.addOnDestinationChangedListener(this)
    }

    override fun onStart() {
        super.onStart()

        // Start our AprsIsService.
        val intent = Intent(this, AprsIsService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)

        launch {
            maybeShowCallsignDialog()
        }

        requestLocation()
    }

    override fun onStop() {
        super.onStop()
        unbindService(mConnection)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        // Workaround for a weird bug!
        val packetHistoryBundle = Bundle()
        packetHistoryBundle.putParcelable(packetHistoryBundleKey, packetHistory)
        outState?.putBundle(packetHistoryBundleKey, packetHistoryBundle)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!)
        return onNavDestinationSelected(item, navController)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            fusedLocationClient.get().lastLocation.addOnSuccessListener { updateAprsIsListener(it) }
        }
    }

    override fun onFragmentInteraction(uri: Uri) {
        // No-op for now.
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private suspend fun maybeShowCallsignDialog() {
        if (preferences.get().getCallsign() == null) {
            callsignDialog.showBlocking(supportFragmentManager, "CallsignDialogFragment", this::runOnUiThread)
        }
    }

    private fun requestLocation() {
        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
    }

    private fun updateAprsIsListener(location: Location?) {
        location ?: return
        val latLng = LatLng(location.latitude, location.longitude)
        aprsIsService?.filter = LocationFilter(latLng, 50.0)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 10001
    }
}
