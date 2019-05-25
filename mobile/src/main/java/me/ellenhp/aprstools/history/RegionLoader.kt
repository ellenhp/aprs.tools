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

import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.google.gson.Gson
import com.google.openlocationcode.OpenLocationCode
import me.ellenhp.aprslib.packet.CacheUpdateCommandPosits
import me.ellenhp.aprstools.ActivityScope
import javax.inject.Inject

@ActivityScope
class RegionLoader @Inject constructor(private val cache: PacketCache, @ActivityScope private val packetRequestQueue: PacketRequestQueue) {

    private val requestsInFlight = HashSet<OpenLocationCode>()

    @Synchronized
    fun ensureRegionLoaded(codes: List<OpenLocationCode>, maxRequests: Int, listener: CacheUpdateListener) {
        val codesNeedingUpdate = cache.updateVisibleCells(codes, listener).minus(requestsInFlight)
        codesNeedingUpdate.minus(requestsInFlight).take(maxRequests).forEach {
            makeRequest(it, listener)
        }
    }

    private fun makeRequest(code: OpenLocationCode, listener: CacheUpdateListener) {
        recordRequestBegin(code)
        val url = "https://api.aprs.tools/within/$code?type=posit"
        val request = JsonObjectRequest(url, null, {
            val command = Gson().fromJson<CacheUpdateCommandPosits>(it.toString(),
                    CacheUpdateCommandPosits::class.java)
            cache.updateCell(code, command, listener)
            recordRequestEnd(code)
        }, {
            recordRequestEnd(code)
        })
        request.retryPolicy = DefaultRetryPolicy(5000, 0, 1f)
        packetRequestQueue.addToRequestQueue(request)
    }

    @Synchronized
    private fun recordRequestBegin(code: OpenLocationCode) {
        requestsInFlight.add(code)
    }

    @Synchronized
    private fun recordRequestEnd(code: OpenLocationCode) {
        requestsInFlight.remove(code)
    }
}