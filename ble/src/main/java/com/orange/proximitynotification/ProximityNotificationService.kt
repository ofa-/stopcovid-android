/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * ProximityNotification foreground service.
 */
abstract class ProximityNotificationService : Service(),
    ProximityNotificationCallback, ProximityPayloadProvider, ProximityPayloadIdProvider,
    ProximityNotificationLogger.Listener,
    CoroutineScope {

    private var bleProximityNotification: BleProximityNotification? = null
    private var bluetoothStateBroadcastReceiver: BluetoothStateBroadcastReceiver? = null

    private val bluetoothRestartInProgress = AtomicBoolean(false)
    val isBluetoothRestartInProgress: Boolean
        get() = bluetoothRestartInProgress.get()

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    abstract val foregroundNotificationId: Int
    abstract val bleSettings: BleSettings
    abstract val exceptionHandler: CoroutineExceptionHandler

    private val bluetoothAdapter: BluetoothAdapter
        get() = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate() {
        super.onCreate()
        ProximityNotificationLogger.registerListener(this)
    }

    override fun onDestroy() {
        stop()
        ProximityNotificationLogger.unregisterListener()
        super.onDestroy()

        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Starts ProximityNotification and foreground service.
     *
     * It registers a [BluetoothStateBroadcastReceiver] in order to be notified of state changes
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
     * It unregisters [BluetoothStateBroadcastReceiver] if any registered
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
     * Notify that [ProximityPayload] provided by [ProximityPayloadProvider] has changed.
     *
     * @param proximityPayload Updated ProximityPayload
     * @see ProximityPayloadProvider
     */
    suspend fun notifyProximityPayloadUpdated(proximityPayload: ProximityPayload) {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_PAYLOAD_UPDATED,
            "Proximity payload updated"
        )

        bleProximityNotification?.notifyPayloadUpdated(proximityPayload)
    }

    /**
     * Notify that [BleSettings] have changed.
     * It will restart [BleProximityNotification] if it is running
     *
     */
    suspend fun notifyBleSettingsUpdate() {
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
     * Force [BleProximityNotification] to restart.
     *
     * If will restart Bluetooth stack if needed by [BleProximityNotification].
     * Otherwise it will restart [BleProximityNotification] by calling notifyBleSettingsUpdate
     */
    suspend fun restart() {

        if (isRunning()) {
            if (bleProximityNotification?.shouldRestartBluetooth == true) {
                restartBluetooth()
            } else {
                notifyBleSettingsUpdate()
            }
        }
    }

    /**
     * @return true if ProximityNotification is running, false otherwise
     */
    fun isRunning() = bleProximityNotification?.isRunning == true

    protected open fun doStart() {
        launch(Dispatchers.Main.immediate + NonCancellable + exceptionHandler) {

            if (!isBluetoothRestartInProgress) {
                startForeground(foregroundNotificationId, buildForegroundServiceNotification())
            }

            startBleProximityNotification()
        }
    }

    protected open fun doStop() {
        launch(Dispatchers.Main.immediate + NonCancellable + exceptionHandler) {
            stopBleProximityNotification()

            if (!isBluetoothRestartInProgress) {
                stopForeground(true)
            }
        }
    }

    private fun registerBluetoothBroadcastReceiver() {
        bluetoothRestartInProgress.set(false)
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
        bluetoothRestartInProgress.set(false)
    }

    private suspend fun startBleProximityNotification() {
        if (bleProximityNotification != null) {
            stopBleProximityNotification()
        }

        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_START_BLE,
            "Start BLE Proximity Notification"
        )

        bleProximityNotification =
            BleProximityNotificationFactory.build(this, bleSettings, this).apply {
                setUp(
                    this@ProximityNotificationService,
                    this@ProximityNotificationService,
                    this@ProximityNotificationService
                )
                start()
            }
    }

    private suspend fun stopBleProximityNotification() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_STOP_BLE,
            "Stop BLE Proximity Notification"
        )

        bleProximityNotification?.stop()
        bleProximityNotification = null
    }

    /**
     * To restart the Bluetooth stack.
     * Beware, on some devices it could show a confirmation popup while disabling / enabling
     * Bluetooth.
     */
    protected suspend fun restartBluetooth() {

        withContext(Dispatchers.Main) {

            if (bluetoothRestartInProgress.getAndSet(true)) {
                ProximityNotificationLogger.info(
                    ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
                    "Restart Bluetooth - already in progress"
                )
            }

            when {
                !bluetoothAdapter.isEnabled -> {
                    val status = bluetoothAdapter.enable()
                    bluetoothRestartInProgress.set(status)

                    ProximityNotificationLogger.info(
                        ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
                        "Restart Bluetooth - enabling (status=$status)"
                    )
                }
                else -> {
                    val status = bluetoothAdapter.disable()
                    bluetoothRestartInProgress.set(status)

                    ProximityNotificationLogger.info(
                        ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
                        "Restart Bluetooth - disabling (status=$status)"
                    )
                }
            }
        }
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

        if (isBluetoothRestartInProgress) {
            launch {
                delay(1000)
                withContext(Dispatchers.Main) {
                    val status = bluetoothAdapter.enable()
                    bluetoothAdapter.cancelDiscovery()

                    ProximityNotificationLogger.info(
                        ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
                        "Restart Bluetooth - enabling on Bluetooth disabled (status=$status)"
                    )

                    bluetoothRestartInProgress.set(status)
                }
            }
        }
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

        if (bluetoothRestartInProgress.getAndSet(false)) {
            ProximityNotificationLogger.info(
                ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
                "Restart Bluetooth - restart bluetooth done"
            )
        }

    }


    override fun onEvent(event: ProximityNotificationEvent) {
    }

    /**
     * Create the Notification to display once this foreground service is running
     *
     * @return Notification used to display foreground service
     */
    abstract fun buildForegroundServiceNotification(): Notification

}


