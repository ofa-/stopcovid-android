/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/24/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.lunabeestudio.stopcovid.coreui.R

enum class CardTheme {

    Default,
    Primary {
        override val themeId: Int = R.style.ShapeAppearance_StopCovid_MediumComponent_Primary
        override val backgroundDrawableRes: Int? = null
    },
    Sick {
        override val themeId: Int = R.style.ShapeAppearance_StopCovid_MediumComponent_Primary
        override val backgroundDrawableRes: Int = R.drawable.bg_sick
    },
    Color {
        override val themeId: Int = R.style.ShapeAppearance_StopCovid_MediumComponent_Color
        override val backgroundDrawableRes: Int = R.drawable.bg_no_risk
    };

    @StyleRes
    open val themeId: Int = R.style.ShapeAppearance_StopCovid_MediumComponent

    @DrawableRes
    open val backgroundDrawableRes: Int? = null
}