/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/14/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.IntDef

@SuppressLint("InlinedApi")
@IntDef(
    flag = true,
    value = [
        PendingIntent.FLAG_ONE_SHOT,
        PendingIntent.FLAG_NO_CREATE,
        PendingIntent.FLAG_CANCEL_CURRENT,
        PendingIntent.FLAG_UPDATE_CURRENT,
        PendingIntent.FLAG_IMMUTABLE,
        PendingIntent.FLAG_MUTABLE,
        Intent.FILL_IN_ACTION,
        Intent.FILL_IN_DATA,
        Intent.FILL_IN_CATEGORIES,
        Intent.FILL_IN_COMPONENT,
        Intent.FILL_IN_PACKAGE,
        Intent.FILL_IN_SOURCE_BOUNDS,
        Intent.FILL_IN_SELECTOR,
        Intent.FILL_IN_CLIP_DATA
    ]
)
@Retention(AnnotationRetention.SOURCE)
annotation class Flags

object ImmutablePendingIntentCompat {
    @SuppressLint("UnspecifiedImmutableFlag")
    fun getActivity(context: Context, requestCode: Int, intent: Intent, @Flags flags: Int = 0): PendingIntent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(context, requestCode, intent, flags or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, requestCode, intent, flags)
        }
}