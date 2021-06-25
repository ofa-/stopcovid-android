/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/18/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.utils

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class ExtendedFloatingActionButtonScrollListener(
    private val floatingActionButton: ExtendedFloatingActionButton
) : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy > SCROLL_DOWN_SHRINK_THRESHOLD && floatingActionButton.isExtended) {
            floatingActionButton.shrink()
        } else if (dy < SCROLL_UP_EXTEND_THRESHOLD && !floatingActionButton.isExtended) {
            floatingActionButton.extend()
        }
    }

    companion object {
        private const val SCROLL_DOWN_SHRINK_THRESHOLD: Int = 10
        private const val SCROLL_UP_EXTEND_THRESHOLD: Int = -30
    }
}