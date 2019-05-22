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

import android.content.Context
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import org.jetbrains.annotations.TestOnly

/**
 *  Base class for fragments in aprs.tools. Subclasses must never call AndroidSupportInjection.inject
 *  manually. Instead they should call super.onAttach() as the first line of their onAttach implementation.
 */
open class AprsToolsFragment : Fragment() {

    private var setupMocksPredicate: (() -> Unit)? = null

    @CallSuper
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (setupMocksPredicate == null) {
            AndroidSupportInjection.inject(this)
        } else {
            setupMocksPredicate?.invoke()
        }
    }

    open fun inject() {
        AndroidSupportInjection.inject(this)
    }

    @TestOnly
    fun setupMocksForTesting(predicate: () -> (Unit)) {
        setupMocksPredicate = predicate
    }
}