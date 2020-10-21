/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification

import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.orange.proximitynotification.ble.BleProximityNotification
import com.orange.proximitynotification.ble.BleProximityNotificationFactory
import com.orange.proximitynotification.ble.BleSettings
import com.orange.proximitynotification.tools.BluetoothStateBroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * ProximityNotification foreground service.
 */
abstract class ProximityNotificationService : Service(),
    ProximityNotificationCallback, ProximityPayloadProvider, ProximityNotificationLogger.Listener,
    CoroutineScope {

    private var bleProximityNotification: BleProximityNotification? = null
    private var bluetoothStateBroadcastReceiver: BluetoothStateBroadcastReceiver? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + SupervisorJob()

    abstract val foregroundNotificationId: Int
    abstract val bleSettings: BleSettings

    override fun onCreate() {
        super.onCreate()
        ProximityNotificationLogger.registerListener(this)
    }

    override fun onDestroy() {
        stop()
        ProximityNotificationLogger.unregisterListener()
        super.onDestroy()

        cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Starts ProximityNotification and foreground service.
     *
     * It registers a BluetoothStateBroadcastReceiver in order to be notified of state changes
     * @see BluetoothStateBroadcastReceiver
     */
    fun start() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_START,
            "Start Proximity Notification"
        )

        registerBluetoothBroadcastReceiver()
        doStart()
    }

    /**
     * Stops ProximityNotification and foreground service.
     *
     * It unregisters a BluetoothStateBroadcastReceiver in order to be notified of state changes
     * @see BluetoothStateBroadcastReceiver
     */
    fun stop() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_STOP,
            "Stop Proximity Notification"
        )

        doStop()
        unregisterBluetoothBroadcastReceiver()
    }

    /**
     * Notify ProximityPayload provided by ProximityPayloadProvider has changed.
     *
     * @see ProximityPayloadProvider
     */
    fun notifyProximityPayloadUpdated() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_PAYLOAD_UPDATED,
            "Proximity payload updated"
        )

        bleProximityNotification?.notifyPayloadUpdated()
    }

    /**
     * Notify that [BleSettings] have changed
     * It will restart [BleProximityNotification]
     */
    fun notifyBleSettingsUpdate() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_BLE_SETTINGS_UPDATED,
            "BLE settings updated"
        )

        if (isRunning()) {
            stopBleProximityNotification()
            startBleProximityNotification()
        }
    }

    /**
     * @return true if ProximityNotification is running, false otherwise
     */
    fun isRunning() = bleProximityNotification?.isRunning == true

    protected open fun doStart() {
        startForeground(foregroundNotificationId, buildForegroundServiceNotification())
        startBleProximityNotification()
    }

    protected open fun doStop() {
        stopBleProximityNotification()
        stopForeground(true)
    }

    private fun registerBluetoothBroadcastReceiver() {
        bluetoothStateBroadcastReceiver = BluetoothStateBroadcastReceiver(
            onBluetoothDisabled = { onBluetoothDisabled() },
            onBluetoothEnabled = { onBluetoothEnabled() })
            .also {
                applicationContext.registerReceiver(
                    it,
                    IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                )
            }
    }

    private fun unregisterBluetoothBroadcastReceiver() {
        bluetoothStateBroadcastReceiver?.let {
            applicationContext.unregisterReceiver(it)
        }
        bluetoothStateBroadcastReceiver = null
    }

    private fun startBleProximityNotification() {
        if (bleProximityNotification != null) {
            stopBleProximityNotification()
        }

        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_START_BLE,
            "Start BLE Proximity Notification"
        )

        BleProximityNotificationFactory.build(this, bleSettings, this)?.let {
            bleProximityNotification = it
            it.setUp(this, this)
            it.start()
        } ?: run {
            onError(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_PROXIMITY_NOTIFICATION,
                    cause = "Failed to initialize bluetooth"
                )
            )
        }
    }

    private fun stopBleProximityNotification() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_STOP_BLE,
            "Stop BLE Proximity Notification"
        )

        bleProximityNotification?.stop()
        bleProximityNotification = null
    }

    /**
     * Called once Bluetooth is disabled.
     * By default it stops the ProximityNotification and the foreground service
     *
     * @see ProximityNotification#stop
     */
    protected open fun onBluetoothDisabled() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_BLUETOOTH_DISABLED,
            "Bluetooth disabled"
        )

        doStop()
    }

    /**
     * Called once Bluetooth is enabled.
     * By default it starts the ProximityNotification and the foreground service
     *
     * @see ProximityNotification#start
     */
    protected open fun onBluetoothEnabled() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_BLUETOOTH_ENABLED,
            "Bluetooth enabled"
        )

        doStart()
    }

    override fun onEvent(event: ProximityNotificationEvent) {
        Timber.d("onEvent event = $event")
    }

    /**
     * Create the Notification to display once this foreground service is running
     *
     * @return Notification used to display foreground service
     */
    abstract fun buildForegroundServiceNotification(): Notification

}


