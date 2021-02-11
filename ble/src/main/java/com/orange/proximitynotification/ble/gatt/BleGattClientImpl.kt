/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/16 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

internal class BleGattClientImpl(
    override val bluetoothDevice: BluetoothDevice,
    private val context: Context
) : BleGattClient, CoroutineScope {

    private val remoteRssiChannel = Channel<ValueWithStatus<Int>>(RENDEZVOUS)
    private val discoveredServicesChannel =
        Channel<ValueWithStatus<List<BluetoothGattService>>>(RENDEZVOUS)
    private val writeCharacteristicChannel =
        Channel<ValueWithStatus<BluetoothGattCharacteristic>>(RENDEZVOUS)
    private val readCharacteristicChannel =
        Channel<ValueWithStatus<BluetoothGattCharacteristic>>(RENDEZVOUS)
    private val connectionStateChannel = Channel<ValueWithStatus<Int>>(CONFLATED)

    private val bluetoothGatt = AtomicReference<BluetoothGatt?>(null)

    private val connectionState = AtomicInteger(BluetoothProfile.STATE_DISCONNECTED)
    private val isClosed = AtomicBoolean(false)

    private val callback: Callback by lazy { Callback() }

    private val isConnected: Boolean
        get() = connectionState.get() == BluetoothProfile.STATE_CONNECTED

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override suspend fun open() {

        // Retrying connection after 133 sometimes works.
        // Never worked on tested devices having SDK < M
        var shouldRetryOn133 = SDK_INT >= M

        do {
            var shouldRetry = false

            connectGatt()
            val result = connectionStateChannel.receive()

            if (result.status == 133) {
                // gatt should have been closed in onConnectionStateChange
                bluetoothGatt.set(null)

                shouldRetry = shouldRetryOn133
                shouldRetryOn133 = false
            }
        } while (shouldRetry)

        check(connectionState.get() == BluetoothProfile.STATE_CONNECTED) { "Expecting bluetoothGatt to be connected" }
    }

    override suspend fun close() {
        internalClose()
    }

    private val closeLock = Mutex()
    private suspend fun internalClose(closeCause: Throwable? = null) = closeLock.withLock {
        if (isClosed.get()) return

        disconnectAndCloseGatt()

        // Close channels
        remoteRssiChannel.close(closeCause)
        discoveredServicesChannel.close(closeCause)
        writeCharacteristicChannel.close(closeCause)
        readCharacteristicChannel.close(closeCause)
        connectionStateChannel.close(closeCause)

        job.cancel()
    }

    private suspend fun disconnectAndCloseGatt() = withContext(NonCancellable) {

        // Disconnect GATT if still connected
        bluetoothGatt.get()?.runCatching {
            if (isConnected) {
                executeOnMain {
                    check(isConnected) { "Expecting bluetoothGatt to be connecting or connected before disconnecting it" }
                    connectionState.set(BluetoothProfile.STATE_DISCONNECTING)
                    bluetoothGatt.get()?.disconnect()
                }
                withTimeout(200) { connectionStateChannel.receive() }
                check(connectionState.get() == BluetoothProfile.STATE_DISCONNECTED) { "Expecting bluetoothGatt to be disconnected" }
            }
        }
        connectionState.set(BluetoothProfile.STATE_DISCONNECTED)

        // Close GATT if not already closed
        bluetoothGatt.get()?.runCatching {
            executeOnMain { bluetoothGatt.getAndSet(null)?.close() }
        }
        bluetoothGatt.set(null)

        isClosed.set(true)
    }

    override suspend fun readRemoteRssi(): Int =
        gattOperation(remoteRssiChannel, "readRemoteRssi") {
            readRemoteRssi()
        }

    override suspend fun discoverServices(): List<BluetoothGattService> =
        gattOperation(discoveredServicesChannel, "discoverServices") {
            discoverServices()
        }

    override suspend fun writeCharacteristic(characteristic: BluetoothGattCharacteristic): BluetoothGattCharacteristic =
        gattOperation(writeCharacteristicChannel, "writeCharacteristic") {
            writeCharacteristic(characteristic)
        }

    override suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic): BluetoothGattCharacteristic =
        gattOperation(readCharacteristicChannel, "readCharacteristic") {
            readCharacteristic(characteristic)
        }

    private inner class Callback : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Timber.d("onConnectionStateChange status=$status, newState=$newState")

            if (isClosed.get()) {
                Timber.w("bluetoothGatt should already be closed")

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.runCatching { disconnect() }
                }
                gatt.runCatching { close() }
                return
            }

            if (status == 133) {
                gatt.runCatching { refreshAndClose() }
            }

            connectionState.set(newState)
            connectionStateChannel.sendValueWithStatus(status, newState)

        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            Timber.d("onReadRemoteRssi status=$status, rssi=$rssi")
            remoteRssiChannel.sendValueWithStatus(status, rssi)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Timber.d("onServicesDiscovered status=$status")
            discoveredServicesChannel.sendValueWithStatus(status, gatt.services)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Timber.d("onCharacteristicWrite status=$status")
            writeCharacteristicChannel.sendValueWithStatus(status, characteristic)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Timber.d("onCharacteristicRead status=$status")
            readCharacteristicChannel.sendValueWithStatus(status, characteristic)
        }
    }

    private suspend fun <T> gattOperation(
        channel: ReceiveChannel<ValueWithStatus<T>>,
        operationName: String,
        operation: BluetoothGatt.() -> Boolean
    ): T {

        if (isClosed.get() || connectionState.get() != BluetoothProfile.STATE_CONNECTED) {
            throw BleGattClientException("operation ($operationName) failed since it is no more connected")
        }

        if (executeOnMain { bluetoothGatt.get()?.operation() } == false) {
            throw BleGattClientException("operation ($operationName) failed to execute")
        }
        val response = channel.receive()

        return response.takeIf { it.status == GATT_SUCCESS }?.value
            ?: run { throw BleGattClientException("operation ($operationName) failed with response=$response") }
    }

    private fun <T> SendChannel<ValueWithStatus<T>>.sendValueWithStatus(status: Int, value: T) {
        if (!isClosedForSend) {
            launch { runCatching { send(ValueWithStatus(status, value)) } }
        }
    }

    private data class ValueWithStatus<out T>(val status: Int, val value: T)

    private suspend inline fun <T> executeOnMain(crossinline operation: suspend () -> T): T {
        return withContext(Dispatchers.Main) { operation() }
    }

    private suspend fun connectGatt() {
        connectionState.set(BluetoothProfile.STATE_CONNECTING)
        val gatt = executeOnMain { bluetoothDevice.connectGattCompat(context, callback) }
        bluetoothGatt.set(checkNotNull(gatt))
    }
}

private fun BluetoothDevice.connectGattCompat(
    context: Context,
    callback: BluetoothGattCallback
): BluetoothGatt? {
    return when {
        SDK_INT >= M -> connectGatt(context, false, callback, TRANSPORT_LE)
        else -> connectGatt(context, false, callback)
    }
}

private fun BluetoothGatt.refreshAndClose() {
    runCatching {
        refreshDeviceCache()
        close()
    }
}

/**
 * Clearing device cache could help recovering from error 133.
 */
private fun BluetoothGatt.refreshDeviceCache() {
    runCatching {
        this.javaClass.getMethod("refresh").invoke(this)
    }
}