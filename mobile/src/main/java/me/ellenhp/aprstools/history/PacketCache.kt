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

package me.ellenhp.aprstools.history

import android.content.Context
import android.util.Log
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
import com.google.openlocationcode.OpenLocationCode
import me.ellenhp.aprslib.packet.CacheUpdateCommandPosits
import me.ellenhp.aprstools.map.PacketPlotter
import java.util.*
import kotlin.collections.HashMap

class PacketCache(private val  context: Context, private val plotter: PacketPlotter) {

    private val cellsInOrder = ArrayDeque<PacketCacheCell>()
    private val allCells = HashMap<OpenLocationCode, PacketCacheCell>()
    private val maxCells = 240
    private val targetCells = 200

    @Synchronized
    fun updateVisibleCells(requestedCodes: List<OpenLocationCode>, maxRequests: Int) {
        if (requestedCodes.size > targetCells) {
            Log.d("cache", "view too big, giving up")
            return
        }

        var updates = 0
        // This code is convoluted, but what we're trying to do is bump the position in the cache
        // for all cells, but only update some/one of them.
        requestedCodes.forEach { code ->
            val cell = getCell(code)
            if (updates < maxRequests) {
                cell.getUpdateUrl()?.let {
                    runUpdate(code, it)
                    updates += 1
                }
            }
        }
        maybeEvictCells()
    }

    private fun maybeEvictCells() {
        if (allCells.count() < maxCells) {
            return
        }
        val cellsToRemove = cellsInOrder.drop(targetCells)
        cellsToRemove.forEach {
            purgeCell(it)
        }
    }

    private fun getCell(cellKey: OpenLocationCode): PacketCacheCell {
        val cell = allCells[cellKey] ?: allocateCell(cellKey)

        cellsInOrder.remove(cell)
        cellsInOrder.push(cell)

        return cell
    }

    @Synchronized
    private fun updateCell(cell: OpenLocationCode, command: CacheUpdateCommandPosits) {
        getCell(cell).update(command, plotter)
    }

    private fun purgeCell(cell: PacketCacheCell) {
        plotter.removeAll(cell.getAllStations())
        allCells.remove(cell.cell)
        cellsInOrder.remove(cell)
    }

    private fun runUpdate(cell: OpenLocationCode, url: String) {
        Log.d("Update", "Issuing HTTP request $url")
        val request = JsonObjectRequest(url, null, {
            val command = Gson().fromJson<CacheUpdateCommandPosits>(it.toString(),
                    CacheUpdateCommandPosits::class.java)
            command?.let { updateCell(cell, command) }
        }, {
            getCell(cell).resetFreshness()
        })
        request.retryPolicy = DefaultRetryPolicy(5000, 5, 1.5f)

        PacketRequestQueue.getInstance(context).addToRequestQueue(request)
    }

    private fun allocateCell(cellKey: OpenLocationCode): PacketCacheCell {
        val cell = PacketCacheCell(cellKey)
        allCells[cellKey] = cell
        cellsInOrder.push(cell)
        return cell
    }

}