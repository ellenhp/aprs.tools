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

package me.ellenhp.aprstools.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager

class EditTextPreferenceWithPreview(context: Context, attributeSet: AttributeSet):
        EditTextPreference(context, attributeSet) {

    private var interceptedListener: OnPreferenceChangeListener? = null

    init {
        // Insert a listener to intercept updates.
        super.setOnPreferenceChangeListener(OnPreferenceChangeListener(this::onTextChanged))
    }

    private fun onTextChanged(preference: Preference, newValue: Any): Boolean {
        val text = newValue as String
        summary = text
        return interceptedListener?.onPreferenceChange(preference, newValue) ?: true
    }

    override fun onAttachedToHierarchy(preferenceManager: PreferenceManager?) {
        super.onAttachedToHierarchy(preferenceManager)
        val currentValue = preferenceManager?.sharedPreferences?.getString(key, null)
        currentValue ?.let { summary = it }
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        return super.onGetDefaultValue(a, index)
    }

    override fun setOnPreferenceChangeListener(onPreferenceChangeListener: OnPreferenceChangeListener?) {
        interceptedListener = onPreferenceChangeListener
    }
}