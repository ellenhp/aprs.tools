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

package me.ellenhp.aprstools.map

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import dagger.Lazy
import javax.inject.Inject
import com.google.openlocationcode.OpenLocationCode
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import me.ellenhp.aprstools.R
import me.ellenhp.aprstools.ZoneUtils
import me.ellenhp.aprstools.history.PacketCache
import me.ellenhp.aprstools.history.PacketCacheFactory
import org.jetbrains.anko.doAsync
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.Instant.now
import javax.inject.Provider

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MapViewFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MapViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MapViewFragment : DaggerFragment(),
        OnMapReadyCallback,
        CoroutineScope by MainScope() {
    private var map: GoogleMap? = null
    private var lastUpdateInstant: Instant? = null
    private var packetCache: PacketCache? = null

    val plotterFactory = PacketPlotterFactory(Provider<Context> { activity!! })
    val packetCacheFactory = PacketCacheFactory(Provider<Context> { activity!! })
    @Inject
    lateinit var fusedLocationClient: Lazy<FusedLocationProviderClient>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_view, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        AndroidSupportInjection.inject(this)

        val mapFragment = SupportMapFragment()
        childFragmentManager.beginTransaction().add(R.id.map_holder, mapFragment).commitNow()
        mapFragment.getMapAsync(this)

        requestPermissions(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val settings = googleMap.uiSettings

        settings.isCompassEnabled = true
        settings.isMyLocationButtonEnabled = true
        settings.isScrollGesturesEnabled = true
        settings.isZoomGesturesEnabled = true
        settings.isTiltGesturesEnabled = false
        settings.isRotateGesturesEnabled = true

        animateToLastLocation()

        val plotter = plotterFactory.create(googleMap)
        packetCache = packetCacheFactory.create(plotter)

        googleMap.setOnCameraMoveListener { loadRegion(true) }
        googleMap.setOnCameraIdleListener {
            loadRegion(false)
            plotter.onCameraIdle()
        }
        map = googleMap
    }

    private fun loadRegion(cameraMoving: Boolean) {
        // Don't even try to figure out all the plus codes we're covering if the zoom is huge.
        if (map!!.cameraPosition.zoom < 4) {
            return
        }

        val bounds = map!!.projection.visibleRegion
        val sw = bounds.latLngBounds.southwest
        val ne = bounds.latLngBounds.northeast

        val lastUpdateSnapshot = lastUpdateInstant
        if (!cameraMoving || (lastUpdateSnapshot == null || now().isAfter(
                        lastUpdateSnapshot.plus(Duration.ofMillis(500))))) {
            lastUpdateInstant = now()
            doAsync {
                val zones = ZoneUtils().getZonesWithin(OpenLocationCode(sw.latitude, sw.longitude).decode(),
                        OpenLocationCode(ne.latitude, ne.longitude).decode())
                packetCache?.updateVisibleCells(zones, if (cameraMoving) 1 else 25)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkSelfPermission(context!!, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                checkSelfPermission(context!!, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            map?.uiSettings?.isMyLocationButtonEnabled = true
            animateToLastLocation()
        }
    }

    private fun animateToLastLocation() {
        if (checkSelfPermission(activity!!, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                checkSelfPermission(activity!!, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
            fusedLocationClient.get()?.lastLocation?.addOnSuccessListener(activity!!) {
                it?.let { map?.animateCamera(newLatLngZoom(LatLng(it.latitude, it.longitude), 10f)) }
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MapViewFragment.
         */
        @JvmStatic
        fun newInstance() =
                MapViewFragment().apply {
                    arguments = Bundle().apply { }
                }

        private const val LOCATION_PERMISSION_REQUEST = 10001
    }
}
