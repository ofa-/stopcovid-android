/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2021/04/21 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.tools

import android.bluetooth.BluetoothAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Wait until Bluetooth state has changed to expected state
 *
 * @param expectedState Expected Bluetooth state
 * @param maxIteration Maximum iteration count (default to 3)
 * @param stateChangeTimeout maximum timeout (in ms) to wait on each iteration for a state change (default to 5s)
 * @param action Action for state change
 *
 * @throws IllegalStateException if [expectedState] was not reached
 */
internal suspend inline fun BluetoothAdapter.waitForState(
    expectedState: Int,
    maxIteration: Int = 3,
    stateChangeTimeout: Long = 5_000L,
    crossinline action: suspend (iteration: Int) -> Unit
) {
    var iteration = 0

    while (iteration < maxIteration && state != expectedState) {
        action(iteration++)
        withTimeoutOrNull(stateChangeTimeout) {
            do {
                delay(100)
            } while (this.isActive && state != expectedState)
        }
    }

    check(state == expectedState) {
        "Unexpected Bluetooth state (current=$state, expected=$expectedState)"
    }
}