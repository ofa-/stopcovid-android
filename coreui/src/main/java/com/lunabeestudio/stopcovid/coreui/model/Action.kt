package com.lunabeestudio.stopcovid.coreui.model

import android.view.View
import androidx.annotation.DrawableRes

data class Action(
    @DrawableRes
    val icon: Int? = null,

    val label: String? = null,
    val showBadge: Boolean = false,
    val onClickListener: View.OnClickListener,
)