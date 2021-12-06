/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/6/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.extension.daysTo
import com.lunabeestudio.stopcovid.extension.fullName
import com.lunabeestudio.stopcovid.extension.midnightDate
import com.lunabeestudio.stopcovid.extension.notificationSent
import com.lunabeestudio.stopcovid.extension.showSmartWallet
import com.lunabeestudio.stopcovid.extension.smartWalletState
import com.lunabeestudio.stopcovid.extension.stringsManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.SmartWallet
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.Date

class SmartWalletNotificationUseCase(
    private val robertManager: RobertManager,
    private val sharedPreferences: SharedPreferences,
    private val getSmartWalletCertificateUseCase: GetSmartWalletCertificateUseCase,
) {

    suspend operator fun invoke(
        applicationContext: Context,
    ) {
        if (
            robertManager.configuration.smartWalletNotif
            && sharedPreferences.showSmartWallet
            && robertManager.configuration.isSmartWalletOn
        ) {
            val smartWallet = getSmartWalletCertificateUseCase().filterNotNull().first()
            val notificationByProfile = getNotificationByProfile(robertManager.configuration, smartWallet)
            val notificationByProfileFiltered = filterNotificationSentByProfile(notificationByProfile)
            val soonestNotification = getSoonestNotifToSend(notificationByProfileFiltered)
            if (soonestNotification != null) {
                sendNotif(applicationContext, soonestNotification)
                registerNotifSent(soonestNotification)
            }
        }
    }

    private fun getNotificationByProfile(
        configuration: Configuration,
        smartWallet: SmartWallet,
    ): Map<String, SmartWalletNotification> {
        val result = mutableMapOf<String, SmartWalletNotification>()
        val today = midnightDate()

        smartWallet.map { (key, certificate) ->
            var smartWalletNotification: SmartWalletNotification? = null
            val smartWalletState = certificate.smartWalletState(configuration)
            val expirationDate = smartWalletState.expirationDate
            if (expirationDate != null) {
                val daysDiff = today.daysTo(expirationDate)
                smartWalletNotification = SmartWalletNotificationType.expirationTypeFromDaysDiff(daysDiff)?.let { expirationType ->
                    certificate.smartWalletNotification(expirationType, expirationDate)
                }
            }
            if (smartWalletNotification == null) {
                val eligibleDate = smartWalletState.eligibleDate
                if (eligibleDate != null) {
                    val daysDiff = today.daysTo(eligibleDate)
                    smartWalletNotification = SmartWalletNotificationType.eligibleTypeFromDaysDiff(daysDiff)?.let { eligibleType ->
                        certificate.smartWalletNotification(eligibleType, eligibleDate)
                    }
                }
            }
            if (smartWalletNotification != null) {
                result[key] = smartWalletNotification
            }
        }

        return result
    }

    private fun filterNotificationSentByProfile(
        notificationByProfile: Map<String, SmartWalletNotification>
    ): Map<String, SmartWalletNotification> {
        val notificationSent = sharedPreferences.notificationSent
        return notificationByProfile.filter { (_, notif) ->
            !notificationSent.contains(notif.getSharedPrefsValue())
        }
    }

    private fun getSoonestNotifToSend(notificationByProfile: Map<String, SmartWalletNotification>): SmartWalletNotification? {
        var soonestNotification: SmartWalletNotification? = null

        notificationByProfile.forEach { (_, smartWalletNotification) ->
            soonestNotification = smartWalletNotification.takeIf {
                val moreImportant = (soonestNotification?.type?.ordinal ?: -1) < smartWalletNotification.type.ordinal
                val equallyImportant = soonestNotification?.type?.ordinal == smartWalletNotification.type.ordinal
                val sooner = soonestNotification?.date?.let {
                    smartWalletNotification.date.before(it)
                } ?: false
                soonestNotification == null || moreImportant || (equallyImportant && sooner)
            } ?: soonestNotification
        }

        return soonestNotification
    }

    private suspend fun sendNotif(
        applicationContext: Context,
        smartWalletNotification: SmartWalletNotification
    ) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (applicationContext.stringsManager().strings.isEmpty()) {
            applicationContext.stringsManager().initialize(applicationContext)
        }
        val strings = applicationContext.stringsManager().strings

        val title = strings[smartWalletNotification.type.titleKey]
        val body = String.format(strings[smartWalletNotification.type.bodyKey] ?: "", smartWalletNotification.name)

        val channelId = UiConstants.Notification.SMART_WALLET.channelId
        val channelName = strings["notification.channel.smartWallet.title"] ?: "Carnet"
        val notificationId = UiConstants.Notification.SMART_WALLET.notificationId

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = NavDeepLinkBuilder(applicationContext)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_main)
            .setDestination(R.id.nav_wallet)
            .createPendingIntent()

        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
            )
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(smartWalletNotification.uvci, notificationId, notification)
    }

    private fun registerNotifSent(smartWalletNotification: SmartWalletNotification) {
        sharedPreferences.notificationSent = sharedPreferences.notificationSent.toMutableSet().apply {
            add(smartWalletNotification.getSharedPrefsValue())
        }
    }

    private fun EuropeanCertificate.smartWalletNotification(type: SmartWalletNotificationType, date: Date) = SmartWalletNotification(
        type = type,
        date = date,
        name = fullName(),
        uvci = greenCertificate.getDgci()
    )

    private class SmartWalletNotification(
        val type: SmartWalletNotificationType,
        val date: Date,
        val name: String,
        val uvci: String,
    ) {
        fun getSharedPrefsValue() = "$uvci${type.prefsKey}"
    }

    private enum class SmartWalletNotificationType(
        val titleKey: String,
        val bodyKey: String,
        val max: Float,
        val min: Float,
        val prefsKey: String,
    ) {
        // /!\ Order is important
        ELIGIBLE_SOON(
            titleKey = "notification.smartWallet.eligibility.title",
            bodyKey = "notification.smartWallet.eligibility.body",
            max = ELG_MAX,
            min = ELG_MIN,
            prefsKey = ELG_SHARED_PREFS_KEY,
        ),
        EXPIRE_SOON_1(
            titleKey = "notification.smartWallet.expiry.title",
            bodyKey = "notification.smartWallet.expiry.body",
            max = EXP_1_MAX,
            min = EXP_1_MIN,
            prefsKey = EXP_1_SHARED_PREFS_KEY,
        ),
        EXPIRE_SOON_2(
            titleKey = "notification.smartWallet.expiry.title",
            bodyKey = "notification.smartWallet.expiry.body",
            max = EXP_2_MAX,
            min = EXP_2_MIN,
            prefsKey = EXP_2_SHARED_PREFS_KEY,
        ),
        EXPIRE_SOON_3(
            titleKey = "notification.smartWallet.expiry.title",
            bodyKey = "notification.smartWallet.expiry.body",
            max = EXP_3_MAX,
            min = EXP_3_MIN,
            prefsKey = EXP_3_SHARED_PREFS_KEY,
        );

        fun contains(daysDiff: Float): Boolean {
            return min < daysDiff && max >= daysDiff
        }

        companion object {
            private val expirationTypes = listOf(EXPIRE_SOON_1, EXPIRE_SOON_2, EXPIRE_SOON_3)
            private val eligibleTypes = listOf(ELIGIBLE_SOON)

            fun expirationTypeFromDaysDiff(daysDiff: Float): SmartWalletNotificationType? =
                expirationTypes.firstOrNull { it.contains(daysDiff) }

            fun eligibleTypeFromDaysDiff(daysDiff: Float): SmartWalletNotificationType? =
                eligibleTypes.firstOrNull { it.contains(daysDiff) }
        }
    }

    companion object {
        private const val EXP_1_MAX: Float = 21f
        private const val EXP_1_MIN: Float = 10f
        private const val EXP_2_MAX: Float = 6f
        private const val EXP_2_MIN: Float = 3f
        private const val EXP_3_MAX: Float = 1f
        private const val EXP_3_MIN: Float = 0f
        private const val ELG_MAX: Float = 15f
        private const val ELG_MIN: Float = 0f
        private const val EXP_1_SHARED_PREFS_KEY: String = "exp1"
        private const val EXP_2_SHARED_PREFS_KEY: String = "exp2"
        private const val EXP_3_SHARED_PREFS_KEY: String = "exp3"
        private const val ELG_SHARED_PREFS_KEY: String = "elg"
    }
}