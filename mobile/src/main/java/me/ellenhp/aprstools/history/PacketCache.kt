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

import com.google.openlocationcode.OpenLocationCode
import me.ellenhp.aprslib.packet.CacheUpdateCommandPosits
import java.util.ArrayDeque
import javax.inject.Inject
import kotlin.collections.HashMap

class PacketCache @Inject constructor() {

    private val cellsInOrder = ArrayDeque<PacketCacheCell>()
    private val allCells = HashMap<OpenLocationCode, PacketCacheCell>()
    private val maxCells = 150
    private val targetCells = 100

    /* Bumps the position in the LRU cache for all visible cells and returns a list of cells that need to be updated */
    @Synchronized
    fun updateVisibleCells(requestedCodes: List<OpenLocationCode>, listener: CacheUpdateListener): List<OpenLocationCode> {
        // Don't even bother if the view is too big, for latency reasons.
        if (requestedCodes.size > targetCells) {
            return listOf()
        }

        val cellsNeedingUpdate = requestedCodes.map { getCell(it) }.filter { it.wantsUpdate() }.map { it.code }
        maybeEvictCells(listener)

        return cellsNeedingUpdate
    }

    @Synchronized
    fun updateCell(cell: OpenLocationCode, command: CacheUpdateCommandPosits, listener: CacheUpdateListener) {
        getCell(cell).update(command, listener)
    }

    private fun maybeEvictCells(listener: CacheUpdateListener) {
        if (allCells.count() < maxCells) {
            return
        }
        val cellsToRemove = cellsInOrder.drop(targetCells)
        cellsToRemove.forEach {
            purgeCell(it, listener)
        }
    }

    private fun getCell(cellKey: OpenLocationCode): PacketCacheCell {
        val cell = allCells[cellKey] ?: allocateCell(cellKey)

        cellsInOrder.remove(cell)
        cellsInOrder.push(cell)

        return cell
    }

    private fun purgeCell(cell: PacketCacheCell, listener: CacheUpdateListener) {
        listener.evictStations(cell.getAllStations())
        allCells.remove(cell.code)
        cellsInOrder.remove(cell)
    }

    private fun allocateCell(cellKey: OpenLocationCode): PacketCacheCell {
        val cell = PacketCacheCell(cellKey)
        allCells[cellKey] = cell
        cellsInOrder.push(cell)
        return cell
    }

    interface CellUpdateListener
}