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

import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.RadioButton
import javax.inject.Inject
import dagger.Lazy
import me.ellenhp.aprstools.AprsToolsApplication
import me.ellenhp.aprstools.R


class BluetoothPromptFragment : DialogFragment() {

    @Inject lateinit var bluetoothAdapter: Lazy<BluetoothAdapter?>

    private var dialogView: View? = null
    private var tncPickerView: ListView? = null

    private val selectedItem: BluetoothDevice?
        get() {
            val adapter = tncPickerView?.adapter as BluetoothDeviceAdapter
            return adapter.selectedItem
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        (activity?.application as AprsToolsApplication).activityComponent?.inject(this)

        val inflater = activity!!.layoutInflater

        dialogView = inflater.inflate(R.layout.bluetooth_prompt_layout, null)
        val builder = AlertDialog.Builder(activity)
                .setTitle(R.string.bluetooth_prompt_label)
                .setCancelable(false)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.done_text), this::onButtonClick)

        tncPickerView = dialogView?.findViewById(R.id.tnc_picker)
        val bluetoothDeviceAdapter = BluetoothDeviceAdapter(activity!!)
        tncPickerView?.adapter = bluetoothDeviceAdapter

        tncPickerView?.setOnItemClickListener { parent, view, position, id -> System.exit(0) }

        bluetoothDeviceAdapter.items = bluetoothAdapter.get()?.bondedDevices?.toList()

        return builder.create()
    }

    private fun onButtonClick(dialog: DialogInterface, which: Int) {
        if (which == Dialog.BUTTON_POSITIVE) {
            activity?.getPreferences(Context.MODE_PRIVATE)?.edit()?.putString(getString(R.string.TNC_BT_ADDRESS), selectedItem?.address)?.apply()
        }
        dismiss()
    }
}
class BluetoothDeviceAdapter(private val context: Context) : BaseAdapter() {

    var items: List<BluetoothDevice>? = listOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selectedItem: BluetoothDevice? = null
    var selectedView: RadioButton? = null

    override fun getItem(position: Int): BluetoothDevice? {
        return items?.get(position)
    }

    override fun getItemId(position: Int): Long {
        return items?.get(position)?.address?.hashCode()?.toLong() ?: 0
    }

    override fun getCount(): Int {
        return items?.size ?: 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val radioButton = convertView as? RadioButton? ?: RadioButton(context)
        radioButton.text = items?.get(position)?.name ?: context.getString(R.string.unknown_tnc)
        radioButton.setOnClickListener {
            view ->
            if (selectedView != null) {
                selectedView?.isChecked = false
            }
            selectedView = view as RadioButton
            selectedItem = getItem(position)
        }
        return radioButton
    }
}
