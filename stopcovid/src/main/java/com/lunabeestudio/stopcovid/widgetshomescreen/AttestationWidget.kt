/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/19/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.widgetshomescreen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.Constants.Url.CERTIFICATE_SHORTCUT_URI
import com.lunabeestudio.stopcovid.Constants.Url.NEW_CERTIFICATE_SHORTCUT_URI
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.isExpired
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class AttestationWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateAttestationWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * Function updating the widget
     */
    private fun updateAttestationWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val barcodeEncoder = BarcodeEncoder()
        val bitmapHelper = BitmapHelper()
        val uriIntent: String
        val views: RemoteViews?

        // get the valid attestations list
        val attestations = context.secureKeystoreDataSource().attestations
        val validAttestations =
            attestations?.filter { !it.isExpired(context.robertManager().configuration) }
                ?.sortedBy { attestation -> attestation.timestamp }
        if (!context.robertManager().configuration.displayAttestation) {
            views = RemoteViews(context.packageName, R.layout.attestation_widget_no_attestation)
            uriIntent = Constants.Url.PROXIMITY_FRAGMENT_URI
            views.setTextViewText(
                R.id.mainTextView,
                StringsManager.strings["attestationsController.endAttestation"] ?: "Attestations plus nécessaires"
            )
        } else if (validAttestations.isNullOrEmpty()) {
            // No certificate valid yet
            views = RemoteViews(context.packageName, R.layout.attestation_widget_no_attestation)
            uriIntent = NEW_CERTIFICATE_SHORTCUT_URI
            views.setTextViewText(
                R.id.mainTextView,
                StringsManager.strings["attestationsController.newAttestation"] ?: "Nouvelle attestation"
            )
        }
        // already created a certif
        else {
            val mainAttestation = validAttestations.last()
            views = RemoteViews(context.packageName, R.layout.attestation_widget_valid_attestation)
            uriIntent = CERTIFICATE_SHORTCUT_URI

            // create QRCode from attestation
            val qrSize = R.dimen.qr_code_widget_size.toDimensSize(context).toInt()
            val qrcodeBitmap = barcodeEncoder.encodeBitmap(
                mainAttestation.qrCode,
                BarcodeFormat.QR_CODE,
                qrSize,
                qrSize
            )
            val dateHour = SimpleDateFormat.getDateInstance(DateFormat.SHORT)
                .format(mainAttestation.timestamp) + ", " + SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
                .format(mainAttestation.timestamp)
            val reason = mainAttestation.widgetString

            // roundCorner on bitmap
            val imageWidget = bitmapHelper.getRoundedBitmap(qrcodeBitmap, R.dimen.qr_widget_corner_radius.toDimensSize(context))
            views.setTextViewText(R.id.dateTextView, dateHour)
            views.setTextViewText(R.id.reasonTextView, reason)
            views.setImageViewBitmap(R.id.qrCodeImageView, imageWidget)
        }
        // setup intent of the widget click
        setIntent(context, views, uriIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    /**
     * set the intent of the click on widget with the uri
     * @param uriIntent is the deeplink of the fragment in the main_graph
     */
    private fun setIntent(context: Context, views: RemoteViews, uriIntent: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(uriIntent)
        )
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.certificateWidgetLayout, pendingIntent)
    }

    companion object {
        /**
         * Is used for asking the widget to update himself
         * @param newCertificate true if you call after creating a certificate, setup a work manager to update the widget once the certificate is no more valid
         * called in NewAttestationFragment & AttestationsFragment
         */
        @OptIn(ExperimentalTime::class)
        fun updateWidget(context: Context, newCertificate: Boolean = false, info: MutableMap<String, FormEntry>? = null) {
            val intent = Intent(context, AttestationWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(
                    ComponentName(
                        context,
                        AttestationWidget::class.java
                    )
                )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)

            // Launch worker for updating when certif no more valid
            if (newCertificate) {
                // if the certificate is set up with in the past
                val timestampAttestation = info?.get(Constants.Attestation.KEY_DATE_TIME)?.value?.toLongOrNull()
                    ?.let(Duration::milliseconds)
                if (timestampAttestation != null) {
                    val attestationDuration = Duration.milliseconds(System.currentTimeMillis()) - timestampAttestation
                    val duration = Duration.hours(context.robertManager().configuration.qrCodeExpiredHours.toDouble())
                    // add 5 minutes to the timing delay to be sure updating the widget after the end of validity
                    val finalDuration = duration + Duration.minutes(5) - attestationDuration
                    val updateAttestationWorker: WorkRequest =
                        OneTimeWorkRequestBuilder<UpdateAttestationWorker>()
                            .setInitialDelay(finalDuration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                            .build()
                    WorkManager
                        .getInstance(context)
                        .enqueue(updateAttestationWorker)
                }
            }
        }
    }
}
