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

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import com.orange.proximitynotification.ble.BleProximityNotification
import com.orange.proximitynotification.ble.BleProximityNotificationFactory
import com.orange.proximitynotification.ble.BleSettings
import com.orange.proximitynotification.tools.BluetoothStateBroadcastReceiver
import com.orange.proximitynotification.tools.waitForState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
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

    private val restartInProgress = AtomicBoolean(false)
    private val bluetoothRestartInProgress = AtomicBoolean(false)
    private var restartBTWakeLock: PowerManager.WakeLock? = null

    val isBluetoothRestartInProgress: Boolean
        get() = bluetoothRestartInProgress.get()

    val couldRestartFrequently: Boolean
        get() = bleProximityNotification?.couldRestartFrequently == true

    val shouldRestart: Boolean
        get() = bleProximityNotification?.shouldRestartBluetooth == true

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    abstract val foregroundNotificationId: Int
    abstract val bleSettings: BleSettings
    abstract val exceptionHandler: CoroutineExceptionHandler

    private val bluetoothAdapter: BluetoothAdapter
        get() = BluetoothAdapter.getDefaultAdapter()

    private val cachedProximityPayloadIdProvider by lazy {
        ProximityPayloadIdProviderWithCache(
            this@ProximityNotificationService,
            maxSize = bleSettings.maxCacheSize,
            expiringTime = bleSettings.identityCacheTimeout
        )
    }

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
     * Starts [ProximityNotification] and foreground service.
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
     * Stops [ProximityNotification] and foreground service.
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
     * Restart [ProximityNotification]
     *
     * If will restart Bluetooth stack if needed by [BleProximityNotification].
     */
    suspend fun restart() {

        if (restartInProgress.get()) {
            ProximityNotificationLogger.info(
                ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART,
                "Restart Proximity Notification already in progress"
            )

            return
        }

        if (bleProximityNotification?.isRunning != true) {
            ProximityNotificationLogger.info(
                ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART,
                "Restart Proximity Notification aborted since it is not running"
            )

            return
        }

        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART,
            "Restart Proximity Notification"
        )

        doRestart()
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

    protected open fun doStart() {
        launch(Dispatchers.Main.immediate + NonCancellable + exceptionHandler) {
            startForeground(foregroundNotificationId, buildForegroundServiceNotification())
            startBleProximityNotification()
        }
    }

    protected open fun doStop() {
        launch(Dispatchers.Main.immediate + NonCancellable + exceptionHandler) {
            stopBleProximityNotification()
            stopForeground(true)
        }
    }

    private suspend fun doRestart() {
        try {
            restartInProgress.set(true)

            val shouldRestartBluetooth =
                bleProximityNotification?.shouldRestartBluetooth == true

            withContext(Dispatchers.Main.immediate) {
                stopBleProximityNotification()
            }

            if (shouldRestartBluetooth) {
                restartBluetooth()
            }

            withContext(Dispatchers.Main.immediate) {
                check(bluetoothAdapter.isEnabled) { "Bluetooth is not enabled" }
                startBleProximityNotification()
            }

            ProximityNotificationLogger.info(
                ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART,
                "Restart Proximity Notification - success"
            )

        } catch (t: Throwable) {
            ProximityNotificationLogger.error(
                ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART,
                "Restart Proximity Notification - failure",
                t
            )

            onError(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_PROXIMITY_NOTIFICATION,
                    cause = "Restart failed (throwable = $t)"
                )
            )

        } finally {
            restartInProgress.set(false)
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
                    cachedProximityPayloadIdProvider,
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
    private suspend fun restartBluetooth(): Boolean {

        ProximityNotificationLogger.info(
            ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
            "Restart Bluetooth"
        )

        try {

            bluetoothRestartInProgress.set(true)
            unregisterBluetoothBroadcastReceiver()

            restartBTWakeLock?.takeIf { it.isHeld }?.release()
            @SuppressLint("WakelockTimeout")
            restartBTWakeLock = (getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "ProximityNotificationService:RestartBTWakeLock"
                )
                ?.apply { acquire() }

            // Disable Bluetooth
            bluetoothAdapter.waitForState(BluetoothAdapter.STATE_OFF) { iteration ->
                val status = withContext(Dispatchers.Main) { bluetoothAdapter.disable() }
                ProximityNotificationLogger.info(
                    ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
                    "Restart Bluetooth - disabling (status=$status, iteration=$iteration)"
                )
            }

            // Enable Bluetooth
            bluetoothAdapter.waitForState(BluetoothAdapter.STATE_ON) { iteration ->
                val status = withContext(Dispatchers.Main) {
                    val isEnabling = bluetoothAdapter.enable()

                    if (isEnabling) {
                        bluetoothAdapter.cancelDiscovery()
                    }

                    isEnabling
                }
                ProximityNotificationLogger.info(
                    ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
                    "Restart Bluetooth - enabling (status=$status, iteration=$iteration)"
                )
            }

            ProximityNotificationLogger.info(
                ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
                "Restart Bluetooth - success"
            )

            return true

        } catch (t: Throwable) {
            ProximityNotificationLogger.error(
                ProximityNotificationEventId.PROXIMITY_NOTIFICATION_RESTART_BLUETOOTH,
                message = "Restart Bluetooth - failure",
                cause = t
            )

            return false

        } finally {

            withContext(NonCancellable) {

                restartBTWakeLock?.takeIf { it.isHeld }?.release()
                restartBTWakeLock = null

                registerBluetoothBroadcastReceiver()
                bluetoothRestartInProgress.set(false)
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
    }

    /**
     * Create the Notification to display once this foreground service is running
     *
     * @return Notification used to display foreground service
     */
    abstract fun buildForegroundServiceNotification(): Notification

}


