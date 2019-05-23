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

package me.ellenhp.aprstools.map.google

import android.app.Activity
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterManager
import com.google.openlocationcode.OpenLocationCode
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprstools.ZoneUtils
import me.ellenhp.aprstools.map.Posit
import me.ellenhp.aprstools.history.RegionLoader
import me.ellenhp.aprstools.map.AprsSymbolTable
import me.ellenhp.aprstools.map.wrapper.MapWrapper
import me.ellenhp.aprstools.map.wrapper.MarkerDescriptor
import org.jetbrains.anko.doAsync
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.Instant.now
import javax.inject.Inject

class GoogleMapWrapper @Inject constructor(
    private val activity: Activity,
    private val loader: RegionLoader
) : MapWrapper, OnMapReadyCallback {
    private val posits = HashMap<Ax25Address, Posit>()
    private val symbolTable = AprsSymbolTable(activity)

    private var lastUpdateInstant: Instant? = null
    private var map: GoogleMap? = null
    private var clusterManager: ClusterManager<Posit>? = null

    override fun init(transitionToMapFragment: (Fragment) -> Unit) {
        val mapFragment = SupportMapFragment()
        transitionToMapFragment.invoke(mapFragment)
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val settings = googleMap.uiSettings

        settings.isCompassEnabled = true
        settings.isMyLocationButtonEnabled = true
        settings.isScrollGesturesEnabled = true
        settings.isZoomGesturesEnabled = true
        settings.isTiltGesturesEnabled = false
        settings.isRotateGesturesEnabled = true

        googleMap.setOnCameraIdleListener {
            clusterManager?.onCameraIdle()
            loadRegionIdle()
        }
        googleMap.setOnCameraMoveListener {
            loadRegionMoving()
        }

        clusterManager = ClusterManager(activity, googleMap)
        clusterManager?.renderer = PositRenderer(activity, googleMap, clusterManager!!)
        map = googleMap
    }

    override fun isReady(): Boolean {
        return map != null
    }

    override fun drawOrUpdateMarker(markerDescriptor: MarkerDescriptor) {
    }

    override fun removeMarker(ax25Address: Ax25Address) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun updateMarkers(descriptors: Collection<MarkerDescriptor>) {
        activity.runOnUiThread {
            descriptors.forEach {
                createOrUpdateMarker(it)
            }
            clusterManager?.cluster()
        }
    }

    override fun evictStations(stations: Collection<Ax25Address>) {
        stations.mapNotNull { posits[it] }.forEach {
            clusterManager?.removeItem(it)
        }
    }

    @Synchronized
    private fun loadRegionIdle() {
        lastUpdateInstant = now()
        val plusCodes = getPlusCodes()
        doAsync {
            loader.ensureRegionLoaded(plusCodes, 25, this@GoogleMapWrapper)
        }
    }

    private fun loadRegionMoving() {
        val lastUpdateSnapshot = lastUpdateInstant
        if ((lastUpdateSnapshot == null || now().isAfter(
                        lastUpdateSnapshot.plus(Duration.ofMillis(1000))))) {
            lastUpdateInstant = now()
            val plusCodes = getPlusCodes()
            doAsync {
                loader.ensureRegionLoaded(plusCodes, 1, this@GoogleMapWrapper)
            }
        }
    }

    private fun getPlusCodes(): List<OpenLocationCode> {
        // Don't even try to figure out all the plus codes we're covering if the zoom is huge.
        if (map!!.cameraPosition.zoom < 4) {
            return listOf()
        }

        val bounds = map!!.projection.visibleRegion
        val sw = bounds.latLngBounds.southwest
        val ne = bounds.latLngBounds.northeast

        return ZoneUtils().getZonesWithin(OpenLocationCode(sw.latitude, sw.longitude).decode(),
                OpenLocationCode(ne.latitude, ne.longitude).decode())
    }

    private fun createOrUpdateMarker(descriptor: MarkerDescriptor) {
        val currentPosit = posits[descriptor.station]
        if (currentPosit == null) {
            val posit = createPosit(descriptor)
            posit?.let { posits[descriptor.station] = it }
            clusterManager?.addItem(posit)
        } else {
            val newPos = LatLng(descriptor.latitude, descriptor.longitude)
            val newSymbol = symbolTable.getSymbol(descriptor.symbol.symbolTable, descriptor.symbol.symbol) ?: return
            currentPosit.symbol = newSymbol
            currentPosit.posit = newPos
            currentPosit.lastHeard = descriptor.timestamp
        }
    }

    private fun createPosit(descriptor: MarkerDescriptor): Posit? {
        val symbol = symbolTable.getSymbol(descriptor.symbol.symbolTable, descriptor.symbol.symbol) ?: return null

        // Locale is deprecated but the recommended replacement doesn't work back to API 21.
        @Suppress("DEPRECATION")
        return Posit(descriptor.station.toString(), LatLng(descriptor.latitude, descriptor.longitude), symbol, descriptor.timestamp, activity.resources.configuration.locale)
    }
}