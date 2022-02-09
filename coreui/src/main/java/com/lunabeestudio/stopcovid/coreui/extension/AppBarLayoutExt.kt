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

import android.view.View
import com.google.android.material.appbar.AppBarLayout
import timber.log.Timber
import java.lang.ref.WeakReference

fun AppBarLayout.setLiftOnScrollTargetView(scrollingView: View) {
    try {
        val field = AppBarLayout::class.java.getDeclaredField("liftOnScrollTargetView")
        field.isAccessible = true
        field.set(this, WeakReference(scrollingView))
        refreshLift(scrollingView)
    } catch (e: Exception) {
        Timber.e(e, "Lift on scroll failed")
    }
}

/**
 * Set the correct elevation state (lift) according to the [scrollingView] scroll position.
 * Should lift test come from AppBarLayout.java::shouldLift (private fun)
 *
 * @param scrollingView
 */
fun AppBarLayout.refreshLift(scrollingView: View) {
    val shouldLift = (scrollingView.canScrollVertically(-1) || scrollingView.scrollY > 0)
    isLifted = shouldLift
}