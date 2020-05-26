/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/18/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.custom

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.utils.LoadingVisibilityDelayDelegate
import kotlinx.android.synthetic.main.layout_content_loading_progress_bar.view.*

class BlockingContentLoadingProgressBar @JvmOverloads constructor(context: Context,
    attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context,
    attrs,
    defStyleAttr) {

    init {
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_content_loading_progress_bar, this, true)
        view.isFocusable = true
        view.isClickable = true
        ViewCompat.setElevation(view, R.dimen.loading_elevation.toDimensSize(context))
    }

    private var storedBackground: Drawable? = null
    private val loadingVisibilityDelayDelegate = LoadingVisibilityDelayDelegate(
        showLoading = {
            visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            background = storedBackground
        },
        hideLoading = {
            visibility = View.GONE
            progressBar.visibility = View.GONE
            background = null
        }
    )

    override fun onFinishInflate() {
        super.onFinishInflate()
        visibility = View.GONE
        progressBar.visibility = View.GONE
        // Store the background for postedShow and nullify it
        storedBackground = background
        background = null
    }

    /**
     * Hide the progress view if it is visible. The progress view will not be
     * hidden until it has been shown for at least a minimum show time. If the
     * progress view was not yet visible, cancels showing the progress view.
     */
    fun hide() {
        loadingVisibilityDelayDelegate.delayHideLoading()
    }

    /**
     * Show the progress view after waiting for a minimum delay. If
     * during that time, hide() is called, the view is never made visible.
     */
    fun show() {
        visibility = View.VISIBLE
        loadingVisibilityDelayDelegate.delayShowLoading()
    }
}
