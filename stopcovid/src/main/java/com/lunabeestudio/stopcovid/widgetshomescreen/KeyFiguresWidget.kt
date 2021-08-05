/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/24/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.widgetshomescreen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.RemoteViews
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.colorStringKey
import com.lunabeestudio.stopcovid.extension.formatNumberIfNeeded
import com.lunabeestudio.stopcovid.extension.labelShortStringKey
import com.lunabeestudio.stopcovid.extension.safeParseColor
import com.lunabeestudio.stopcovid.extension.setupStyleWidget
import com.lunabeestudio.stopcovid.fastitem.NumbersCardItem
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.model.ChartData
import com.lunabeestudio.stopcovid.model.KeyFigure
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class KeyFiguresWidget : AppWidgetProvider() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        GlobalScope.launch(Dispatchers.Main) {
            // load strings and figures on startup
            if (KeyFiguresManager.featuredFigures == null || KeyFiguresManager.highlightedFigures == null) {
                KeyFiguresManager.initialize(context)
            }
            if (StringsManager.strings.isNullOrEmpty()) {
                StringsManager.initialize(context)
            }
            appWidgetIds.forEach { appWidgetId ->
                updateFiguresWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    fun updateFiguresWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.key_figures_widget)
        val strings = StringsManager.strings
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
        // Set title widget
        views.setTextViewText(
            R.id.titleWidgetTextView,
            strings["home.infoSection.keyFigures"]
        )
        // Highlighted Figures
        displayHighlightedFigure(strings, numberFormat, views, context)
        // Featured Figures
        displayFeaturedFigures(strings, context, views, numberFormat)
        // adapt layout with widget size
        setStyleWidgetWithSize(appWidgetManager, views, appWidgetId, context)
        setIntent(context, views)
        appWidgetManager.updateAppWidget(appWidgetId, views)
        UpdateKeyFiguresWorker.scheduleWorker(context)
    }

    private fun displayFeaturedFigures(
        strings: LocalizedStrings,
        context: Context,
        views: RemoteViews,
        numberFormat: NumberFormat,
    ) {
        KeyFiguresManager.featuredFigures?.let { keyFigures ->
            val data = NumbersCardItem.Data(
                strings["common.country.france"],
                keyFigures.getOrNull(0)?.let { generateFromKeyFigure(it, context, strings, numberFormat) },
                keyFigures.getOrNull(1)?.let { generateFromKeyFigure(it, context, strings, numberFormat) },
                keyFigures.getOrNull(2)?.let { generateFromKeyFigure(it, context, strings, numberFormat) }
            )

            views.apply {
                setTextViewText(R.id.labelFigure1TextView, data.dataFigure1?.label)
                setTextViewText(R.id.labelFigure2TextView, data.dataFigure2?.label)
                setTextViewText(R.id.labelFigure3TextView, data.dataFigure3?.label)
                setTextViewText(R.id.valueFigure1TextView, data.dataFigure1?.value)
                setTextViewText(R.id.valueFigure2TextView, data.dataFigure2?.value)
                setTextViewText(R.id.valueFigure3TextView, data.dataFigure3?.value)
                data.dataFigure1?.color?.let { setTextColor(R.id.labelFigure1TextView, it) }
                data.dataFigure2?.color?.let { setTextColor(R.id.labelFigure2TextView, it) }
                data.dataFigure3?.color?.let { setTextColor(R.id.labelFigure3TextView, it) }
            }
        }
    }

    fun generateFromKeyFigure(
        keyFigure: KeyFigure,
        context: Context,
        strings: LocalizedStrings,
        numberFormat: NumberFormat,
    ): NumbersCardItem.DataFigure {
        return NumbersCardItem.DataFigure(
            strings[keyFigure.labelShortStringKey],
            keyFigure.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat),
            strings[keyFigure.colorStringKey(context.isNightMode())].safeParseColor()
        )
    }

    private fun displayHighlightedFigure(
        strings: LocalizedStrings,
        numberFormat: NumberFormat,
        views: RemoteViews,
        context: Context
    ) {
        KeyFiguresManager.highlightedFigures?.let { figure ->
            val label = "${strings[figure.labelShortStringKey]} (${strings["common.country.france"]})"
            val value = figure.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
            val colorFigure = strings[figure.colorStringKey(context.isNightMode())].safeParseColor()
            val chartData = globalData(figure, context)
            val bitmapGraph = chartData?.let { createLineChart(context, it) }

            views.apply {
                setTextViewText(R.id.labelMainFigureTextView, label)
                setTextColor(R.id.labelMainFigureTextView, colorFigure)
                setTextViewText(R.id.valueMainFigureTextView, value)
                setImageViewBitmap(R.id.graphImageView, bitmapGraph)
                setInt(R.id.graphImageView, "setColorFilter", colorFigure)
            }
        }
    }

    private fun globalData(figure: KeyFigure, context: Context) =
        figure.series?.takeLast(Constants.HomeScreenWidget.NUMBER_VALUES_GRAPH_FIGURE)
            ?.let { series ->
                ChartData(
                    description = StringsManager.strings["common.country.france"],
                    currentValueToDisplay = figure.valueGlobalToDisplay,
                    entries = series
                        .sortedBy { it.date }
                        .map { Entry(it.date.toFloat(), it.value.toFloat()) },
                    color = StringsManager.strings[figure.colorStringKey(context.isNightMode())].safeParseColor()
                )
            }

    private fun createLineChart(
        context: Context,
        chartData: ChartData,
    ): Bitmap? {
        // init layout LineChart
        val figureLineChart = LineChart(context)
        figureLineChart.apply {
            layoutParams = RelativeLayout.LayoutParams(R.dimen.widget_figures_graph_size, R.dimen.widget_figures_graph_size)
            measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            layout(0, 0, figureLineChart.measuredWidth, figureLineChart.measuredHeight)
        }

        val lineDataSet = chartData.let {
            LineDataSet(it.entries, it.description).apply {
                setupStyleWidget(it.color)
            }
        }

        figureLineChart.apply {
            data = LineData(lineDataSet)
            setupStyleWidget()
        }
        return getBitmapFromView(figureLineChart)
    }

    fun getBitmapFromView(graph: View): Bitmap? {
        val returnedBitmap = Bitmap.createBitmap(
            graph.measuredWidth,
            graph.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(returnedBitmap)
        canvas.drawColor(Color.TRANSPARENT)
        graph.draw(canvas)
        return returnedBitmap
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        context?.let { updateWidget(it) }
    }

    private fun setStyleWidgetWithSize(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetId: Int,
        context: Context
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        if (options != null) {
            val widgetHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            if (widgetHeight > R.dimen.widget_2_row_min_height.toDimensSize(context)) {
                // 2 row Widget
                val fontSize = context.resources.getDimension(R.dimen.widget_main_figure_font_size)
                views.apply {
                    setViewVisibility(R.id.secondFiguresLayout, View.VISIBLE)
                    setViewVisibility(R.id.actionDividerTitleMainLayout, View.VISIBLE)
                    setTextViewTextSize(
                        R.id.valueMainFigureTextView,
                        TypedValue.COMPLEX_UNIT_PX,
                        fontSize
                    )
                }
            } else {
                // 1 row widget
                val fontSize =
                    context.resources.getDimension(R.dimen.widget_second_figure_font_size)
                views.apply {
                    setViewVisibility(R.id.secondFiguresLayout, View.GONE)
                    setViewVisibility(R.id.actionDividerTitleMainLayout, View.GONE)
                    setTextViewTextSize(
                        R.id.valueMainFigureTextView,
                        TypedValue.COMPLEX_UNIT_PX,
                        fontSize
                    )
                }
            }
        }
    }

    private fun setIntent(context: Context, views: RemoteViews) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(Constants.Url.FIGURES_FRAGMENT_URI)
        )
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.figuresWidgetLayout, pendingIntent)
    }

    companion object {
        fun updateWidget(context: Context) {
            val intent = Intent(context, KeyFiguresWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, KeyFiguresWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
