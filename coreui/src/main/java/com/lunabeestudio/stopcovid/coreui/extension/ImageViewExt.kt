/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible

fun ImageView.setImageResourceOrHide(@DrawableRes resId: Int?) {
    resId?.let(this::setImageResource)
    isVisible = resId != null
}