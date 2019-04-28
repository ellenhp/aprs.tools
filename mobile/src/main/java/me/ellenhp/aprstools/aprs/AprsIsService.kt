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

package me.ellenhp.aprstools.aprs

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import me.ellenhp.aprslib.packet.AprsPacket
import me.ellenhp.aprstools.settings.AprsIsServerAddress
import me.ellenhp.aprstools.AprsToolsApplication
import me.ellenhp.aprstools.settings.Preferences
import javax.inject.Inject
import javax.inject.Provider
import dagger.Lazy

class AprsIsService : Service() {

    @Inject
    lateinit var preferences: Lazy<Preferences?>
    @Inject
    lateinit var clientFactory: AprsIsClientFactory
    @Inject
    lateinit var aprsIsThreadProvider: Provider<AprsIsThread>

    private val binder = AprsIsServiceBinder()

    var filter: LocationFilter? = null
        set(value) {
            field = value
            resetClient()
        }

    private var thread: AprsIsThread? = null

    override fun onBind(intent: Intent?): IBinder? {
        (application as AprsToolsApplication).activityComponent?.inject(this)

        if (thread == null || thread?.isAlive == true) {
            thread = aprsIsThreadProvider.get()
            resetClient()
            thread?.start()
        } else {
            resetClient()
        }
        return binder
    }

    fun sendPacket(packet: AprsPacket) {
        thread?.enqueuePacket(packet)
    }

    private fun resetClient() {
        val credentials = preferences.get()?.getAprsIsCredentials() ?: return
        val serverAddress = preferences.get()?.getAprsIsServerAddress() ?: return
        thread?.setClient(clientFactory.create(
                serverAddress.host,
                serverAddress.port,
                credentials.call,
                credentials.passcode,
                filter))
    }

    inner class AprsIsServiceBinder : Binder() {
        fun getService(): AprsIsService {
            return this@AprsIsService
        }
    }

}

