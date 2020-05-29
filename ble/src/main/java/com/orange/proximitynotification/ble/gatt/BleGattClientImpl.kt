/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/16 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.SendChannel

internal class BleGattClientImpl(
    override val bluetoothDevice: BluetoothDevice,
    private val context: Context
) : BleGattClient {

    companion object {
        private val TAG = BleGattClientImpl::class.java.simpleName
    }

    private val remoteRssiChannel = Channel<ValueWithStatus<Int>>(CONFLATED)
    private val connectionStateChannel = Channel<ValueWithStatus<Int>>(CONFLATED)

    private var _isConnected = false
    override val isConnected: Boolean
        get() = !isClosed && _isConnected

    private var isClosed = false

    private var bluetoothGatt: BluetoothGatt? = null

    override suspend fun open() {
        bluetoothGatt = bluetoothDevice.connectGattCompat(context, Callback())
        connectionStateChannel.receive() // suspends until connectionStateChanged is received
        check(isConnected)
    }

    override suspend fun close() {
        if (!isClosed) {
            doClose()
        }
    }

    private fun doClose() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()

        isClosed = true
        _isConnected = false

        remoteRssiChannel.close()
        connectionStateChannel.close()
    }

    override suspend fun readRemoteRssi(): Int {
        if (bluetoothGatt?.readRemoteRssi() == false) {
            throw BleGattClientException("Failed requesting for read remote RSSI")
        }

        val response = remoteRssiChannel.receive()  // suspends until remoteRssi is received
        return response.takeIf { it.status == GATT_SUCCESS }?.value
            ?: run { throw BleGattClientException("Failed reading remote RSSI ($response)") }
    }

    private inner class Callback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange status=$status, newState=$newState")
            _isConnected = status == GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED

            when (status) {
                GATT_SUCCESS -> connectionStateChannel.safeOffer(ValueWithStatus(status, newState))
                else -> doClose()
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            Log.d(TAG, "onReadRemoteRssi status=$status, rssi=$rssi")
            remoteRssiChannel.safeOffer(ValueWithStatus(status = status, value = rssi))
        }
    }

    private data class ValueWithStatus<out T>(val status: Int, val value: T)
}

private fun <E> SendChannel<E>.safeOffer(element: E) {
    runCatching {
        offer(element)
    }
}

private fun BluetoothDevice.connectGattCompat(
    context: Context,
    callback: BluetoothGattCallback
): BluetoothGatt {
    return when {
        SDK_INT >= M -> connectGatt(context, false, callback, TRANSPORT_LE)
        else -> connectGatt(context, false, callback)
    }
}