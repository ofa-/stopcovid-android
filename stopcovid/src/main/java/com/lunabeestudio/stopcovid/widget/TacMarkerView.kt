/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/09/13 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.lunabeestudio.stopcovid.databinding.InitLayoutTacMarkerViewBinding
import com.lunabeestudio.stopcovid.extension.formatCompact
import com.lunabeestudio.stopcovid.extension.getRelativeDateShortString
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@SuppressLint("ViewConstructor")
class TacMarkerView(context: Context, chart: Chart<*>) : ConstraintLayout(context), IMarker {

    private var mWeakChart: WeakReference<Chart<*>> = WeakReference(chart)

    var chart: Chart<*>?
        set(value) {
            mWeakChart = WeakReference(value)
        }
        get() = mWeakChart.get()

    private val binding: InitLayoutTacMarkerViewBinding by lazy {
        InitLayoutTacMarkerViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override fun refreshContent(e: Entry, highlight: Highlight?) {
        val yLabel = e.y.formatCompact(context)
        val xLabel = e.x.toLong().seconds.getRelativeDateShortString(context)
        binding.markerTextView.text = listOf(yLabel, xLabel).joinToString("\n")

        chart?.data?.getDataSetForEntry(e)?.color?.let {
            (binding.markerTextView.background as GradientDrawable).color = ColorStateList.valueOf(it)
            binding.arrowBottomImageView.imageTintList = ColorStateList.valueOf(it)
            binding.arrowTopImageView.imageTintList = ColorStateList.valueOf(it)
        }

        measureLayout()
    }

    override fun getOffset(): MPPointF = MPPointF((-(width / 2)).toFloat(), (-height).toFloat())

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        val offset = MPPointF(offset.x, offset.y)

        val width = width.toFloat()
        val height = height.toFloat()
        val chartWidth = chart?.width ?: 0
        val chartHeight = chart?.height ?: 0

        var markerAbove = true

        if (posX + offset.x < 0) {
            offset.x = -posX
        } else if (chart != null && posX + width + offset.x > chartWidth) {
            offset.x = chartWidth - posX - width
        }

        if (posY + offset.y < 0) {
            offset.y = 0f
            markerAbove = false
        } else if (chart != null && posY + height + offset.y > chartHeight) {
            offset.y = chartHeight - posY - height
        }

        if (width != 0f) {
            val bias = abs(offset.x / width)
            binding.contentLayout.setHorizontalBias(binding.arrowTopImageView.id, bias)
            binding.contentLayout.setHorizontalBias(binding.arrowBottomImageView.id, bias)
        }
        binding.arrowBottomImageView.isVisible = markerAbove
        binding.arrowTopImageView.isVisible = !markerAbove
        measureLayout()

        return offset
    }

    override fun draw(canvas: Canvas, posX: Float, posY: Float) {
        val offset = getOffsetForDrawingAtPoint(posX, posY)
        val saveId = canvas.save()
        // translate to the correct position and draw
        canvas.translate(posX + offset.x, posY + offset.y)
        draw(canvas)
        canvas.restoreToCount(saveId)
    }

    private fun measureLayout() {
        measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        layout(0, 0, measuredWidth, measuredHeight)
    }

    private fun ConstraintLayout.setHorizontalBias(
        @IdRes targetViewId: Int,
        bias: Float
    ) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.setHorizontalBias(targetViewId, bias)
        constraintSet.applyTo(this)
    }
}