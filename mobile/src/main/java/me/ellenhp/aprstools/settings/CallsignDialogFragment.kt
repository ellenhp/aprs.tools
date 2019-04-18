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

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.widget.EditText
import me.ellenhp.aprstools.PreferenceKeys
import me.ellenhp.aprstools.R

class CallsignDialogFragment : AprsToolsDialogFragment() {

    private var dialogView: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity?.layoutInflater

        dialogView = inflater?.inflate(R.layout.callsign_prompt_layout, null)
        val builder = AlertDialog.Builder(activity)
                .setTitle(R.string.callsign_prompt_title)
                .setCancelable(false)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.done_text), this::onButtonClick)
        return builder.create()
    }

    private fun onButtonClick(dialog: DialogInterface, which: Int) {
        if (which != Dialog.BUTTON_POSITIVE) {
            return
        }
        val callsignEditText = dialogView?.findViewById<EditText>(R.id.callsign_edit_text)
        val callsign = callsignEditText?.text.toString()
        val preferences = activity?.getPreferences(Activity.MODE_PRIVATE)
        // TODO validate input here?
        preferences?.edit()?.putString(PreferenceKeys.CALLSIGN, callsign)?.apply()
    }
}
