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
import com.google.android.gms.location.FusedLocationProviderClient
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import me.ellenhp.aprstools.AprsToolsFragment
import me.ellenhp.aprstools.R
import me.ellenhp.aprstools.history.PacketCache
import me.ellenhp.aprstools.map.wrapper.MapWrapper
import org.threeten.bp.Instant

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MapViewFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MapViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MapViewFragment : AprsToolsFragment(),
        CoroutineScope by MainScope() {
    private var lastUpdateInstant: Instant? = null
    private var packetCache: PacketCache? = null

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject
    lateinit var mapWrapper: MapWrapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_view, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val applyMapTransition = { fragment: Fragment ->
            childFragmentManager.beginTransaction().add(R.id.map_holder, fragment).commitNow()
        }

        mapWrapper.init(applyMapTransition)

        requestPermissions(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST)
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
