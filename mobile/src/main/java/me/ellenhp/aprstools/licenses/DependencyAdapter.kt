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
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.ellenhp.aprstools.R

class DependencyAdapter(val context: Context, private val dependencies: List<Dependency>): RecyclerView.Adapter<DependencyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DependencyViewHolder {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.license_list_item, parent, false)
        return DependencyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dependencies.count()
    }

    override fun onBindViewHolder(holder: DependencyViewHolder, position: Int) {
        holder.bindData(dependencies[position])
    }


}