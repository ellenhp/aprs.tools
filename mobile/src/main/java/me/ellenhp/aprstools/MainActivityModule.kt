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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import dagger.Binds
import dagger.multibindings.ClassKey

@Module(subcomponents = [MainActivitySubcomponent::class])
abstract class MainActivityModule {
    @Binds
    @IntoMap
    @ClassKey(MainActivity::class)
    abstract fun bindYourActivityInjectorFactory(factory: MainActivitySubcomponent.Factory): AndroidInjector.Factory<*>
}

@Module
class MainActivityConcreteModule {
    @Provides
    @ActivityScope
    fun providesContext(activity: MainActivity): Context {
        return activity
    }

    @Provides
    @ActivityScope
    fun providesSharedPreferences(activity: MainActivity): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(activity)
    }

    @Provides
    @ActivityScope
    fun providesBluetoothAdapter(activity: MainActivity): BluetoothAdapter? {
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    @Provides
    @ActivityScope
    fun provideLocationProviderClient(activity: MainActivity): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(activity)
    }
}