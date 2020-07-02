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
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.framework.ble.service.RobertProximityService
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.robert.model.BLEAdvertiserException
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.orange.proximitynotification.ProximityInfo
import com.orange.proximitynotification.ble.BleProximityMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.pow

class ProximityService : RobertProximityService() {

    var onError: ((RobertException) -> Unit)? = null

    private var lastError: RobertException? = null

    private val binder = ProximityBinder()

    private val strings = StringsManager.getStrings(this)

    override val robertManager: RobertManager by lazy {
        robertManager()
    }

    override fun onProximity(proximityInfo: ProximityInfo) {
        sendNotification(proximityInfo)
        updateDisseminatedEbids()
        (applicationContext as RobertApplication).notifyListener(proximityInfo)
        super.onProximity(proximityInfo)
    }

    private fun sendNotification(proximityInfo: ProximityInfo) {
        val calibratedRssi = (proximityInfo.metadata as BleProximityMetadata).calibratedRssi
        val rssi1m = -70
        val bleAtt = 2.2
        val estimatedDistance = 10.0.pow((rssi1m - calibratedRssi) / (10.0 * bleAtt))

        val message = "rssi: " + calibratedRssi + "dBm, dist: %.1f".format(estimatedDistance) +"m"

        sendNotification(message)
    }

    private var lastEbid: EphemeralBluetoothIdentifier? = null
    private fun updateDisseminatedEbids() {
        val currentEbid = (robertManager as RobertManagerImpl).getCurrentEbid()
            ?: return
        if (lastEbid == currentEbid)
            return
        lastEbid = currentEbid
        CoroutineScope(Dispatchers.Default).launch {
            saveDisseminatedEbid(currentEbid)
        }
    }

    private fun saveDisseminatedEbid(it: EphemeralBluetoothIdentifier) {
        val file = (robertManager as RobertManagerImpl).disseminatedEbidsFile
        synchronized(file) {
            file.appendText(listOf(
                it.epochId,
                it.ntpStartTimeS,
                it.ntpEndTimeS,
                Base64.encodeToString(it.ecc, Base64.NO_WRAP),
                Base64.encodeToString(it.ebid, Base64.NO_WRAP),
                "\n"
            ).joinToString(" "))
        }
    }

    override val foregroundNotificationId: Int = UiConstants.Notification.PROXIMITY.notificationId

    override fun buildForegroundServiceNotification(): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.PROXIMITY.channelId,
                strings["notification.channel.title"],
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.cancel(UiConstants.Notification.BLUETOOTH.notificationId)
        notificationManager.cancel(UiConstants.Notification.ERROR.notificationId)
        return buildNotification(strings["notification.proximityServiceRunning.title"])
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

    private fun buildNotification(message: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        return NotificationCompat.Builder(this,
            UiConstants.Notification.PROXIMITY.channelId
        )
            .setContentTitle(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun sendNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(message)
        notificationManager.notify(UiConstants.Notification.PROXIMITY.notificationId, notification)
    }

    private fun sendErrorNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.ERROR.channelId,
                strings["notification.channel.error.title"],
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

    private fun sendErrorBluetoothNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.BLUETOOTH.channelId,
                strings["notification.channel.error.title"],
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
            UiConstants.Notification.BLUETOOTH.channelId
        )
            .setContentTitle(strings["notification.error.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(strings["notification.error.connectivity"]))
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(UiConstants.Notification.BLUETOOTH.notificationId, notification)
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, ProximityService::class.java)
        fun start(context: Context): Unit = ContextCompat.startForegroundService(context, intent(context))
        fun stop(context: Context): Boolean = context.stopService(intent(context))
    }
}
