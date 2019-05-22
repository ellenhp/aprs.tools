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

package me.ellenhp.aprstools.licenses

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import me.ellenhp.aprstools.R

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AprsToolsLicenseFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AprsToolsLicenseFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class AprsToolsLicenseFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_aprs_tools_license, container, false)
        view.findViewById<TextView>(R.id.aprstools_gpl_textview).text =
                activity!!.assets.open("APRSToolsLicense.txt").reader().readText()
        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AprsToolsLicenseFragment.
         */
        @JvmStatic
        fun newInstance() =
                AprsToolsLicenseFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
