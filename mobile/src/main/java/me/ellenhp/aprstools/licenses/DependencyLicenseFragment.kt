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

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import me.ellenhp.aprstools.R

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [DependencyLicenseFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [DependencyLicenseFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class DependencyLicenseFragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_dependency_license, container, false)
        val packagedDeps = Dependency.parseFile(activity!!.assets.open("packaged_deps.yml").reader().readText())
        val deps = Dependency.parseFile(activity!!.assets.open("licenses.yml").reader().readText())
        val adapter = DependencyAdapter(context!!, packagedDeps!! + deps!!)
        val recyclerView = view.findViewById<RecyclerView>(R.id.dependency_list_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment DependencyLicenseFragment.
         */
        @JvmStatic
        fun newInstance() =
                DependencyLicenseFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
