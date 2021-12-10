package com.lunabeestudio.stopcovid.coreui.model

import android.view.View
import androidx.annotation.DrawableRes

data class Action(
    @DrawableRes
    val icon: Int? = null,

    val label: String? = null,
    val showBadge: Boolean = false,
    val loading: Boolean = false,
    val showArrow: Boolean = true,
    val onClickListener: View.OnClickListener?,
)