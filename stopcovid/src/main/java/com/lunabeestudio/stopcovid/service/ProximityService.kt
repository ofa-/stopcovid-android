/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lunabeestudio.framework.ble.service.RobertProximityService
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.BLEAdvertiserException
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class ProximityService : RobertProximityService() {

    var onError: ((RobertException) -> Unit)? = null

    private var lastError: RobertException? = null

    private val binder = ProximityBinder()

    private val strings: HashMap<String, String>
        get() = StringsManager.strings

    override val robertManager: RobertManager by lazy {
        robertManager()
    }

    override val foregroundNotificationId: Int = UiConstants.Notification.PROXIMITY.notificationId

    override fun buildForegroundServiceNotification(): Notification = runBlocking {
        if (strings.isEmpty()) {
            StringsManager.initialize(this@ProximityService)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.PROXIMITY.channelId,
                strings["notification.channel.title"] ?: "Collecte active",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this@ProximityService, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this@ProximityService, 0,
            notificationIntent, 0
        )
        notificationManager.cancel(UiConstants.Notification.BLUETOOTH.notificationId)
        notificationManager.cancel(UiConstants.Notification.ERROR.notificationId)
        NotificationCompat.Builder(
            this@ProximityService,
            UiConstants.Notification.PROXIMITY.channelId
        )
            .setContentTitle(strings["notification.proximityServiceRunning.title"])
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setNotificationSilent()
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(strings["notification.proximityServiceRunning.message"])
            )
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        robertManager.deactivateProximity(applicationContext as RobertApplication)
        super.onDestroy()
    }

    override fun onError(error: RobertException) {
        if (error is BLEAdvertiserException && !ProximityManager.isBluetoothOn(this)) {
            sendErrorBluetoothNotification()
            doStop()
        } else {
            Timber.e(error)
            sendErrorNotification()
            robertManager.deactivateProximity(applicationContext as RobertApplication)
            lastError = error
            onError?.invoke(error)
        }
    }

    fun consumeLastError(): RobertException? {
        val error = lastError
        lastError = null
        return error
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return binder
    }

    inner class ProximityBinder : Binder() {
        fun getService(): ProximityService = this@ProximityService
    }

    private fun sendErrorNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            if (strings.isEmpty()) {
                StringsManager.initialize(this@ProximityService)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    UiConstants.Notification.ERROR.channelId,
                    strings["notification.channel.error.title"] ?: "Erreur",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(this@ProximityService, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this@ProximityService, 0,
                notificationIntent, 0
            )
            val notification = NotificationCompat.Builder(
                this@ProximityService,
                UiConstants.Notification.ERROR.channelId
            )
                .setContentTitle(strings["notification.error.title"])
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification_bar)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(strings["notification.error.message"])
                )
                .setContentIntent(pendingIntent)
                .build()
            notificationManager.notify(UiConstants.Notification.ERROR.notificationId, notification)
        }
    }

    override fun sendErrorBluetoothNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            if (strings.isEmpty()) {
                StringsManager.initialize(this@ProximityService)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    UiConstants.Notification.BLUETOOTH.channelId,
                    strings["notification.channel.error.title"] ?: "Erreur",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(this@ProximityService, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this@ProximityService, 0,
                notificationIntent, 0
            )
            val notification = NotificationCompat.Builder(
                this@ProximityService,
                UiConstants.Notification.BLUETOOTH.channelId
            )
                .setContentTitle(strings["notification.error.title"])
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_notification_bar)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(strings["notification.error.connectivity"])
                )
                .setContentIntent(pendingIntent)
                .build()
            notificationManager.notify(UiConstants.Notification.BLUETOOTH.notificationId, notification)
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, ProximityService::class.java)
        fun start(context: Context): Unit = ContextCompat.startForegroundService(context, intent(context))
        fun stop(context: Context): Boolean = context.stopService(intent(context))
    }
}
