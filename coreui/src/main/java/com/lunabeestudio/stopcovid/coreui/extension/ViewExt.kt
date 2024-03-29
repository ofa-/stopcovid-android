/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.lunabeestudio.stopcovid.coreui.R

/**
 * Set a window insets listener and apply insets.systemWindowInsetBottom as bottom padding to the view and consume it
 */
fun View.applyAndConsumeWindowInsetBottom() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        view.updatePadding(
            bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom.takeIf { it != 0 }
                ?: insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        )

        WindowInsetsCompat.Builder(insets)
            .setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    0
                )
            )
            .build()
    }
}

/**
 * Register the view that the [appBarLayout] should use to determine whether it should be lifted.
 *
 * @param appBarLayout The appBar to lift on scroll.
 */
fun View.registerToAppBarLayoutForLiftOnScroll(appBarLayout: AppBarLayout) {
    // Ensure lift code is called after view is on screen
    appBarLayout.post {
        appBarLayout.setLiftableOverrideEnabled(false)
        appBarLayout.isLiftOnScroll = true
        appBarLayout.setLiftOnScrollTargetView(this)
    }
}

fun View.hideBottomSheet() {
    isVisible = false
}

fun View.showBottomSheet() {
    isVisible = true
}

fun View.showSnackBar(
    message: String,
    duration: Int = Snackbar.LENGTH_LONG,
    errorSnackBar: Boolean = false,
) {
    this.showSnackBar(message, duration) {
        if (errorSnackBar) {
            setBackgroundTint(ContextCompat.getColor(context, R.color.color_error))
            setTextColor(ContextCompat.getColor(context, R.color.color_on_error))
        } else {
            setTextColor(R.attr.colorSurface.fetchSystemColor(context))
        }
    }
}

inline fun View.showSnackBar(
    message: String,
    length: Int = Snackbar.LENGTH_LONG,
    f: Snackbar.() -> Unit,
) {
    val snack = Snackbar.make(this, message, length)
    snack.f()
    snack.show()
}

fun View.addRipple(): Unit = with(TypedValue()) {
    context.theme.resolveAttribute(R.attr.selectableItemBackground, this, true)
    setBackgroundResource(resourceId)
}

fun View.setOnClickListenerOrHideRipple(onClickListener: View.OnClickListener?) {
    if (onClickListener != null) {
        addRipple()
    } else {
        background = null
    }
    setOnClickListener(onClickListener)
}
