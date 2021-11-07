package com.lunabeestudio.stopcovid.widgetshomescreen

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/26/7 - for the TOUS-ANTI-COVID project
 */
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.RemoteViews
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.extension.stringsManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class DccWidget : AppWidgetProvider() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        GlobalScope.launch(Dispatchers.Main) {
            // load strings and on startup
            if (context.stringsManager().strings.isNullOrEmpty()) {
                context.stringsManager().initialize(context)
            }
            appWidgetIds.forEach { appWidgetId ->
                updateDccWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    /**
     * Function updating the widget
     */
    private suspend fun updateDccWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views: RemoteViews?
        val cryptoManager = LocalCryptoManager(context)
        val favCertificate = try {
            SecureKeystoreDataSource(context, cryptoManager, null).rawWalletCertificates().firstOrNull { it.isFavorite }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

        if (favCertificate != null) {
            views = RemoteViews(context.packageName, R.layout.dcc_widget)
            val qrBitmap = generateBarcodeFromCertificate(favCertificate, context)
            views.apply {
                setImageViewBitmap(R.id.dccQrCodeImageView, qrBitmap)
                setTextViewText(
                    R.id.titleWidgetTextView,
                    context.stringsManager().strings["walletController.favoriteCertificateSection.title"]
                )
                setTextViewText(
                    R.id.captionQrCodeTextView,
                    context.stringsManager().strings["widget.dcc.full"] ?: "Appuyez pour passer en plein écran"
                )
            }
            val uriIntent: String = Constants.Url.DCC_FULLSCREEN_SHORTCUT_URI + favCertificate.id
            setIntent(context, views, uriIntent)
        } else {
            views = RemoteViews(context.packageName, R.layout.dcc_widget_no_fav_widget)
            views.apply {
                setTextViewText(
                    R.id.infoTextView,
                    context.stringsManager().strings["widget.dcc.empty"]
                        ?: "Ajoutez ici votre certificat favori en appuyant sur l’icône ❤️ sur le certificat (au format européen) souhaité."
                )
                setTextViewText(R.id.titleWidgetTextView, context.stringsManager().strings["app.name"])
            }
            val uriIntent: String = Constants.Url.WALLET_CERTIFICATE_SHORTCUT_URI
            setIntent(context, views, uriIntent)
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun generateBarcodeFromCertificate(certificate: RawWalletCertificate, context: Context): Bitmap {
        val barcodeEncoder = BarcodeEncoder()
        val qrSize = R.dimen.qr_code_widget_dcc_size.toDimensSize(context).toInt()

        return barcodeEncoder.encodeBitmap(
            certificate.value,
            BarcodeFormat.QR_CODE,
            qrSize,
            qrSize
        )
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
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.dccWidgetLayout, pendingIntent)
    }

    companion object {
        /**
         * Is used for asking the widget to update himself
         */
        fun updateWidget(context: Context) {
            val intent = Intent(context, DccWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(
                    ComponentName(
                        context,
                        DccWidget::class.java
                    )
                )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}