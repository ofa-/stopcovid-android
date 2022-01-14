/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.lunabeestudio.stopcovid.R

enum class CertificateHeaderState(
    @ColorRes val backgroundColor: Int,
    @ColorRes val textColor: Int,
    @DrawableRes val icon: Int,
) {
    INFO(
        backgroundColor = R.color.color_mountain_meadow,
        textColor = R.color.color_white_85,
        icon = R.drawable.ic_info,
    ),
    WARNING(
        backgroundColor = R.color.color_alert,
        textColor = R.color.color_black_55,
        icon = R.drawable.ic_warning,
    ),
    ERROR(
        backgroundColor = R.color.color_error,
        textColor = R.color.color_white_85,
        icon = R.drawable.ic_warning,
    )
}