/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
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
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.robertManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ProximityService : RobertProximityService() {

    var onError: ((RobertException) -> Unit)? = null

    private var lastError: RobertException? = null

    private val binder = ProximityBinder()

    private val strings: HashMap<String, String> by lazy {
        if (StringsManager.getStrings().isEmpty()) {
            StringsManager.init(this)
        }
        StringsManager.getStrings()
    }

    private var statusUpdateSchedulerScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override val robertManager: RobertManager by lazy {
        robertManager()
    }

    override val foregroundNotificationId: Int = UiConstants.Notification.PROXIMITY.notificationId

    override fun buildForegroundServiceNotification(): Notification {
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

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        return NotificationCompat.Builder(this,
            UiConstants.Notification.PROXIMITY.channelId
        )
            .setContentTitle(strings["notification.proximityServiceRunning.title"])
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setNotificationSilent()
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(strings["notification.proximityServiceRunning.message"]))
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleNextStatusUpdate()
        return START_STICKY
    }

    private fun scheduleNextStatusUpdate() {
        statusUpdateSchedulerScope.launch {
            val nextStatusUpdateDelay = TimeUnit.HOURS.toMillis(1L)
            Timber.d("Next status update in ${nextStatusUpdateDelay}ms")
            delay(nextStatusUpdateDelay)
            if (isActive) {
                (applicationContext as StopCovid).refreshStatusIfNeeded()
            }
        }.invokeOnCompletion {
            if (isActive) {
                scheduleNextStatusUpdate()
            }
        }
    }

    override fun onDestroy() {
        robertManager.deactivateProximity(applicationContext as RobertApplication)
        statusUpdateSchedulerScope.cancel()
        super.onDestroy()
    }

    override fun onError(error: RobertException) {
        Timber.e(error)
        sendErrorNotification()
        robertManager.deactivateProximity(applicationContext as RobertApplication)
        lastError = error
        onError?.invoke(error)
        statusUpdateSchedulerScope.cancel()
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.ERROR.channelId,
                strings["notification.channel.error.title"] ?: "Erreur",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this,
            UiConstants.Notification.ERROR.channelId
        )
            .setContentTitle(strings["notification.error.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(strings["notification.error.message"]))
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(UiConstants.Notification.ERROR.notificationId, notification)
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, ProximityService::class.java)
        fun start(context: Context): Unit = ContextCompat.startForegroundService(context, intent(context))
        fun stop(context: Context): Boolean = context.stopService(intent(context))
    }
}