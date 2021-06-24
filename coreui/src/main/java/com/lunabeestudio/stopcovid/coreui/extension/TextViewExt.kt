package com.lunabeestudio.stopcovid.coreui.extension

import android.view.View
import android.widget.TextView

fun TextView.setTextOrHide(
    value: String?,
    ifVisibleBlock: (TextView.() -> Unit)? = null
) {
    if (value.isNullOrEmpty()) {
        visibility = View.GONE
    } else {
        text = value.safeEmojiSpanify()
        ifVisibleBlock?.let { apply(it) }
        visibility = View.VISIBLE
    }
}