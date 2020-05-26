/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.tools

import android.bluetooth.BluetoothAdapter.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothStateBroadcastReceiver(
    private val onBluetoothEnabled: () -> Unit,
    private val onBluetoothDisabled: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra(EXTRA_STATE, STATE_OFF)
        val previousState = intent.getIntExtra(EXTRA_PREVIOUS_STATE, STATE_OFF)

        when (state) {
            STATE_ON -> onBluetoothEnabled()
            STATE_TURNING_OFF,
            STATE_OFF -> if (previousState != STATE_TURNING_OFF && previousState != STATE_OFF)
                onBluetoothDisabled()
        }
    }
}