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
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat.*
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.*
import com.google.android.gms.maps.CameraUpdateFactory.*
import com.google.android.gms.maps.model.LatLng
import dagger.Lazy
import me.ellenhp.aprslib.packet.Ax25Address
import me.ellenhp.aprstools.history.HistoryUpdateListener
import me.ellenhp.aprstools.history.PacketTrackHistory
import me.ellenhp.aprstools.map.PacketPlotter
import me.ellenhp.aprstools.map.PacketPlotterFactory
import org.threeten.bp.Duration
import javax.inject.Inject


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MapViewFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MapViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MapViewFragment : Fragment(), OnMapReadyCallback, HistoryUpdateListener {
    private var listener: OnFragmentInteractionListener? = null
    private var map: GoogleMap? = null

    private lateinit var plotter: PacketPlotter

    @Inject
    lateinit var fusedLocationClient: Lazy<FusedLocationProviderClient>
    @Inject
    lateinit var packetHistory: Lazy<PacketTrackHistory>
    @Inject
    lateinit var plotterFactory: PacketPlotterFactory

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_view, container, false)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }

        (activity?.application as AprsToolsApplication).activityComponent!!.inject(this)

        val mapFragment = SupportMapFragment()
        childFragmentManager.beginTransaction().add(R.id.map_holder, mapFragment).commitNow()
        mapFragment.getMapAsync(this)

    }

    override fun onDetach() {
        super.onDetach()
        listener = null
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

        plotter = plotterFactory.create(map, Duration.ofHours(6))

        animateToLastLocation()

        plotter.plot(packetHistory.get())
        packetHistory.get().listener = this

        activity!!.runOnUiThread {
            plotter.plot(packetHistory.get())
        }
    }

    private fun animateToLastLocation() {
        if (checkSelfPermission(activity!!, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED ||
                checkSelfPermission(activity!!, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED)
            fusedLocationClient.get()?.lastLocation?.addOnSuccessListener(activity!!) {
                map?.animateCamera(newLatLngZoom(LatLng(it.latitude, it.longitude), 10f))
            }
    }

    override fun historyUpate(station: Ax25Address) {
        activity?.runOnUiThread {
            plotter.plot(packetHistory.get())
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
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
    }
}
