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

package me.ellenhp.aprstools

import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class AprsToolsDialogFragment() : DialogFragment() {

    enum class DialogResult {
        DISMISSED
    }

    var continuation: Continuation<DialogResult>? = null

    @Synchronized
    suspend fun showBlocking(manager: FragmentManager?, tag: String?) {
        suspendCoroutine(fun(it: Continuation<DialogResult>) {
            continuation = it
            show(manager, tag)
        })
    }

    @Synchronized
    override fun dismiss() {
        continuation?.resume(DialogResult.DISMISSED)
        super.dismiss()

    }
}