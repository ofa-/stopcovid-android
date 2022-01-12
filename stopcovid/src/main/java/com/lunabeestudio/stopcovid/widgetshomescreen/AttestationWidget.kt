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
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLocale
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.utils.ImmutablePendingIntentCompat
import com.lunabeestudio.stopcovid.extension.isExpired
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.stringsManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class AttestationWidget : AppWidgetProvider() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        GlobalScope.launch(Dispatchers.Main) {
            appWidgetIds.forEach { appWidgetId ->
                updateAttestationWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    /**
     * Function updating the widget
     */
    private suspend fun updateAttestationWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val barcodeEncoder = BarcodeEncoder()
        val bitmapHelper = BitmapHelper()
        val uriIntent: String
        val views: RemoteViews?

        // get the valid attestations list
        val attestations = context.secureKeystoreDataSource().attestations()
        val validAttestations =
            attestations.data?.filter { !it.isExpired(context.robertManager().configuration) }
                ?.sortedBy { attestation -> attestation.timestamp }
        if (!context.robertManager().configuration.displayAttestation) {
            views = RemoteViews(context.packageName, R.layout.attestation_widget_no_attestation)
            uriIntent = Constants.Url.PROXIMITY_FRAGMENT_URI
            views.setTextViewText(
                R.id.mainTextView,
                context.stringsManager().strings["attestationsController.endAttestation"] ?: "Attestations plus nécessaires"
            )
        } else if (validAttestations.isNullOrEmpty()) {
            // No certificate valid yet
            views = RemoteViews(context.packageName, R.layout.attestation_widget_no_attestation)
            uriIntent = NEW_CERTIFICATE_SHORTCUT_URI
            views.setTextViewText(
                R.id.mainTextView,
                context.stringsManager().strings["attestationsController.newAttestation"] ?: "Nouvelle attestation"
            )
        }
        // already created an attestation
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
            val dateHour = SimpleDateFormat.getDateInstance(DateFormat.SHORT, context.getApplicationLocale())
                .format(mainAttestation.timestamp) + ", " + SimpleDateFormat.getTimeInstance(
                DateFormat.SHORT,
                context.getApplicationLocale()
            )
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
        val pendingIntent = ImmutablePendingIntentCompat.getActivity(context, 0, intent)
        views.setOnClickPendingIntent(R.id.certificateWidgetLayout, pendingIntent)
    }

    companion object {
        /**
         * Request widget update
         *
         * @param isNewCertificate true if you call after creating a certificate, setup a work manager to update the widget once the
         * certificate is no more valid.
         */
        fun updateWidget(context: Context, isNewCertificate: Boolean = false, info: MutableMap<String, FormEntry>? = null) {
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

            // Launch worker for updating when certificate no more valid
            if (isNewCertificate) {
                // if the certificate is set up with in the past
                val timestampAttestation = info?.get(Constants.Attestation.KEY_DATE_TIME)?.value?.toLongOrNull()?.milliseconds
                if (timestampAttestation != null) {
                    val attestationDuration = System.currentTimeMillis().milliseconds - timestampAttestation
                    val duration = context.robertManager().configuration.qrCodeExpiredHours.toDouble().hours
                    // add 5 minutes to the timing delay to be sure updating the widget after the end of validity
                    val finalDuration = duration + 5.minutes - attestationDuration
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
