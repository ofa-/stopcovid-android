package com.lunabeestudio.stopcovid.extension

import android.widget.TextView
import androidx.core.view.isVisible

fun TextView.setTextOrHide(text: CharSequence?) {
    setText(text)
    isVisible = text != null
}