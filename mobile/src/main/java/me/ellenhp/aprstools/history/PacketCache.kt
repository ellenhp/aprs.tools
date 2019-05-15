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

import android.util.Log
import com.google.gson.Gson
import com.google.openlocationcode.OpenLocationCode
import me.ellenhp.aprslib.packet.CacheUpdateCommand
import me.ellenhp.aprstools.map.PacketPlotter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.doAsync

class PacketCache(private val plotter: PacketPlotter) {

    private var head: PacketCacheCell? = null
    private val allCells = HashMap<OpenLocationCode, PacketCacheCell>()
    private val maxCells = 35
    private val targetCells = 30


    @Synchronized
    fun updateVisibleCells(requestedCodes: List<OpenLocationCode>) {
        if (requestedCodes.size > targetCells * 2) {
            allCells.values.toList().forEach { purgeCell(it) }
            return
        }
        if (requestedCodes.size > targetCells * 0.75) {
            allCells.values.forEach { it.setHidden(true, plotter) }
            return
        }

        val codesToHide = allCells.keys.minus(requestedCodes)
        codesToHide.map { allCells[it] }.forEach { it?.setHidden(true, plotter) }
        requestedCodes.map { allCells[it] }.forEach { it?.setHidden(false, plotter) }

        requestedCodes.forEach { requestUpdate(it) }
    }

    private fun requestUpdate(cellKey: OpenLocationCode) {
        Log.d("Update", "Updating cell $cellKey")
        val cell = allCells.getOrDefault(cellKey, null) ?: allocateCell(cellKey)

        // Remove cell from linked list.
        cell.prev?.next = cell.next
        cell.next?.prev = cell.prev

        // Insert cell at head.
        if (cell != head)
            cell.next = head
        cell.prev = null
        head?.prev = cell
        head = cell

        cell.getUpdateUrl()?.let {
            doAsync {
                getUpdateCommand(it)?.let {
                    maybeUpdateCell(cell, it)
                }
            }
        }

        if (allCells.count() < maxCells) {
            return
        }
        Log.d("evicting", "evicting cells because we have ${allCells.count()}")
        var cur = head
        for (i in 0..targetCells) {
            Log.d("evicting", "not evicting cell ${cur?.cell}")
            cur = cur?.next
        }
        while (cur != null) {
            Log.d("evicting", "evicting cell ${cur.cell}")
            val tmp = cur.next
            purgeCell(cur)
            cur = tmp
        }
        Log.d("evicting", "we now have ${allCells.count()}")
    }

    @Synchronized
    private fun maybeUpdateCell(cell: PacketCacheCell, command: CacheUpdateCommand) {
        if (allCells.containsKey(cell.cell)) {
            cell.update(command, plotter)
        }
    }

    private fun purgeCell(cell: PacketCacheCell) {
        plotter.removeAll(cell.getAllStations())
        allCells.remove(cell.cell)
        cell.prev?.next = null
        cell.next?.prev = null
        cell.prev = null
        cell.next = null
    }

    private fun getUpdateCommand(url: String): CacheUpdateCommand? {
        val client = OkHttpClient()
        Log.d("Update", "Issuing HTTP request $url")
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        Log.d("Update", "HTTP request $url came back with status ${response.code()}")
        if (!response.isSuccessful || response.code() != 200) {
            return null
        }
        val body = response.body()
        val command = body?.string()?.let { Gson().fromJson<CacheUpdateCommand>(it, CacheUpdateCommand::class.java) }
        body?.close()
        return command
    }

    private fun allocateCell(cellKey: OpenLocationCode): PacketCacheCell {
        val cell = PacketCacheCell(cellKey)
        allCells[cellKey] = cell
        return cell
    }

}