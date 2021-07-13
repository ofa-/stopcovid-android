/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/20/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.widgetshomescreen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants.Url.PROXIMITY_FRAGMENT_URI
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.manager.RisksLevelManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Implementation of App Widget functionality.
 */
class ProximityWidget : AppWidgetProvider() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Sometimes strings are not loaded
        GlobalScope.launch(Dispatchers.Main) {
            if (StringsManager.strings.isNullOrEmpty()) {
                StringsManager.initialize(context)
            }
            appWidgetIds.forEach { appWidgetId ->
                updateProximityWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle?) {
        context?.let { updateWidget(it) }
    }

    private fun updateProximityWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val strings = StringsManager.strings
        val views = RemoteViews(context.packageName, R.layout.proximity_widget)
        val robertManager = context.robertManager()
        setStyleWidgetWithSize(appWidgetManager, views, appWidgetId, context)
        views.setViewVisibility(R.id.infoTextView, View.VISIBLE)
        // --- UPDATE RISK LEVEL ---
        // if user not registered
        if (!robertManager.isRegistered) {
            displayNotRegistered(views, strings)
        }
        // if the user is sick
        else if (robertManager.isImmune) {
            displaySick(views, strings)
        }
        // Display the risk level if not sick
        else if (robertManager.atRiskStatus?.riskLevel != null) {
            displayRisk(views, robertManager, strings)
        } else {
            displayNoRisk(views, strings)
        }
        // update Proximity status
        views.setTextViewText(R.id.titleWidgetTextView, getProximityText(context, strings, robertManager))
        // setIntent widget click
        setIntent(context, views)
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun displayNotRegistered(views: RemoteViews, strings: LocalizedStrings) {
        views.apply {
            setInt(R.id.riskLayout, "setBackgroundResource", R.drawable.bg_widget_risk_1)
            setTextViewText(R.id.riskTextView, strings["error.cryptoIssue.explanation.register"])
            setTextViewText(R.id.infoTextView, strings["common.error.needRegister"])
        }
    }

    private fun displaySick(views: RemoteViews, strings: LocalizedStrings) {
        views.apply {
            setInt(R.id.riskLayout, "setBackgroundResource", R.drawable.bg_widget_sick)
            setTextViewText(R.id.riskTextView, strings["home.healthSection.isSick.standaloneTitle"])
            // no info for sick status
            setViewVisibility(R.id.infoTextView, View.GONE)
        }
    }

    private fun displayRisk(views: RemoteViews, robertManager: RobertManager, strings: LocalizedStrings) {
        RisksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.let {
            views.apply {
                setTextViewText(R.id.riskTextView, strings[it.labels.homeTitle])
                setTextViewText(R.id.infoTextView, strings[it.labels.homeSub])
                when (it.riskLevel) {
                    in 3.0..4.0 -> {
                        setInt(R.id.riskLayout, "setBackgroundResource", R.drawable.bg_widget_risk_3)
                    }
                    in 2.0..3.0 -> {
                        setInt(R.id.riskLayout, "setBackgroundResource", R.drawable.bg_widget_risk_2)
                    }
                    in 1.0..2.0 -> {
                        setInt(R.id.riskLayout, "setBackgroundResource", R.drawable.bg_widget_risk_1)
                    }
                    in 0.0..1.0 -> {
                        setInt(R.id.riskLayout, "setBackgroundResource", R.drawable.bg_widget_risk_0)
                    }
                    else -> {
                        setInt(R.id.riskLayout, "setBackgroundResource", R.drawable.bg_widget_risk_4)
                    }
                }
            }
        }
    }

    private fun displayNoRisk(views: RemoteViews, strings: LocalizedStrings) {
        views.apply {
            setInt(R.id.riskLayout, "setBackgroundResource", R.drawable.bg_widget_risk_0)
            setTextViewText(R.id.riskTextView, strings["risk0.home.title"])
            setTextViewText(R.id.infoTextView, strings["risk0.home.sub"])
        }
    }

    private fun setStyleWidgetWithSize(appWidgetManager: AppWidgetManager, views: RemoteViews, appWidgetId: Int, context: Context) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        if (options != null) {
            val widthWidget = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val heightWidget = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            views.apply {
                if (heightWidget > R.dimen.widget_2_row_min_height.toDimensSize(context)) {
                    // 2 row Widget height
                    setInt(R.id.riskTextView, "setMaxLines", 10)
                    setInt(R.id.infoTextView, "setMaxLines", 10)
                    setInt(R.id.textLayout, "setGravity", Gravity.NO_GRAVITY)
                    // 4 column Widget width
                    if (widthWidget > R.dimen.widget_4_column_min_width.toDimensSize(context)) {
                        setInt(R.id.textLayout, "setGravity", Gravity.CENTER_VERTICAL)
                        setViewVisibility(R.id.riskImageView, View.VISIBLE)
                    } else {
                        // 3 column Widget width
                        setViewVisibility(R.id.riskImageView, View.GONE)
                        setInt(R.id.textLayout, "setGravity", Gravity.NO_GRAVITY)
                    }
                } else {
                    // 1 row widget height
                    setInt(R.id.riskTextView, "setMaxLines", 2)
                    setInt(R.id.infoTextView, "setMaxLines", 1)
                    setViewVisibility(R.id.riskImageView, View.GONE)
                }
            }
        }
    }

    private fun getProximityText(context: Context, strings: LocalizedStrings, robertManager: RobertManager): SpannableString {
        val stringSpan: SpannableString
        if (robertManager.isProximityActive && ProximityManager.isBluetoothOn(context, robertManager)) {
            val phrase = strings["widget.proximity.enabled.main"] ?: "TousAntiCovid est %s"
            val coloredString = strings["widget.proximity.enabled.second"] ?: "activé"
            val stringFinal = String.format(phrase, coloredString)
            val indexStartColored = stringFinal.indexOf(coloredString)
            stringSpan = SpannableString(stringFinal)
            stringSpan.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.color_no_risk)),
                indexStartColored,
                indexStartColored + coloredString.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            val phrase = strings["widget.proximity.disabled.main"] ?: "TousAntiCovid est %s"
            val coloredString = strings["widget.proximity.disabled.second"] ?: "désactivé"
            val stringFinal = String.format(phrase, coloredString)
            val indexStartColored = stringFinal.indexOf(coloredString)
            stringSpan = SpannableString(stringFinal)
            stringSpan.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.color_monza)),
                indexStartColored,
                indexStartColored + coloredString.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return stringSpan
    }

    private fun setIntent(context: Context, views: RemoteViews) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(PROXIMITY_FRAGMENT_URI)
        )
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.clickableWidgetLayout, pendingIntent)
    }

    companion object {
        fun updateWidget(context: Context) {
            val intent = Intent(context, ProximityWidget::class.java)
            intent.action = ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(
                    ComponentName(
                        context,
                        ProximityWidget::class.java
                    )
                )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
