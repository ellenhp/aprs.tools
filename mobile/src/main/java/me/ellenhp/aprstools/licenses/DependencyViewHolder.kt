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

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.ellenhp.aprstools.R

class DependencyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    fun bindData(dependency: Dependency) {
        // These should always be present
        view.findViewById<TextView>(R.id.dep_name).text = dependency.name!!
        view.findViewById<TextView>(R.id.dep_artifact).text = dependency.artifact!!

        if (dependency.copyrightHolder == null) {
            view.findViewById<TextView>(R.id.dep_copyright_holder).visibility = View.INVISIBLE
        } else {
            view.findViewById<TextView>(R.id.dep_copyright_holder).text = dependency.copyrightHolder
        }

        if (dependency.license == null) {
            view.findViewById<View>(R.id.dep_license_name_layout).visibility = View.INVISIBLE
        } else {
            view.findViewById<TextView>(R.id.dep_license_name).text = dependency.license
        }

        if (dependency.licenseUrl == null) {
            view.findViewById<View>(R.id.dep_license_link_layout).visibility = View.INVISIBLE
        } else {
            view.findViewById<TextView>(R.id.dep_license_link).text = dependency.licenseUrl
        }

        if (dependency.url == null) {
            view.findViewById<View>(R.id.dep_link_layout).visibility = View.INVISIBLE
        } else {
            view.findViewById<TextView>(R.id.dep_link).text = dependency.url
        }
    }
}