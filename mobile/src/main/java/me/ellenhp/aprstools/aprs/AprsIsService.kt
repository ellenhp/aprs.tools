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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.ellenhp.aprstools.aprs

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class AprsIsService : Service() {

    val binder = AprsIsServiceBinder()

    var host: String? = null
        set(value) {
            field = value
            resetClient()
        }

    var port: Int? = null
        set(value) {
            field = value
            resetClient()
        }

    var filter: LocationFilter? = null
        set(value) {
            field = value
            resetClient()
        }

    var callsign: String? = null
        set(value) {
            field = value
            resetClient()
        }

    var listener: AprsIsListener? = null
        set(value) {
            field = value
            if (value != null)
                thread?.listener = value
        }

    private var thread: AprsIsThread? = null

    // TODO support multiple listeners
    override fun onBind(intent: Intent?): IBinder? {
        if (thread?.isAlive != true) {
            thread = AprsIsThread(listener)
            resetClient()
            thread?.start()
        }
        else {
            thread?.listener = listener
            resetClient()
        }
        return binder
    }

    inner class AprsIsServiceBinder : Binder() {
        fun getService(): AprsIsService {
            return this@AprsIsService
        }
    }

    private fun resetClient() {
        thread?.setClient(AprsIsClient(
                host ?: return,
                port ?: return,
                callsign ?: return,
                filter))
    }

}

