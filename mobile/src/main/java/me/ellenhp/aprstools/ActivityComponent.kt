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

import dagger.Subcomponent
import me.ellenhp.aprstools.map.MapViewFragment
import me.ellenhp.aprstools.modules.TimeModule
import me.ellenhp.aprstools.settings.BluetoothPromptFragment
import me.ellenhp.aprstools.settings.CallsignDialogFragment
import me.ellenhp.aprstools.tracker.TrackerService

@ActivityScope
@Subcomponent(modules = [TimeModule::class])
interface ActivityComponent {

    fun inject(mainActivity: MainActivity)
    fun inject(dialogFragment: BluetoothPromptFragment)
    fun inject(dialogFragment: CallsignDialogFragment)
    fun inject(trackerService: TrackerService)
    fun inject(mapViewFragment: MapViewFragment)
}
