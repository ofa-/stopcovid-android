/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.text.isDigitsOnly
import timber.log.Timber
import java.text.NumberFormat
import java.util.regex.Pattern

fun String.openInExternalBrowser(context: Context) {
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(this))
    context.startActivity(browserIntent)
}

fun String.isCodeValid(): Boolean {
    return length == 6 || Pattern.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", this)
}

fun String.isPostalCode(): Boolean {
    return length == 5 && this.isDigitsOnly()
}

fun String.attestationLabelFromKey() = "attestation.form.$this.label"

fun String.attestationPlaceholderFromKey() = "attestation.form.$this.placeholder"

fun String.attestationShortLabelFromKey() = "attestation.form.$this.shortLabel"

fun String.attestationLongLabelFromKey() = "attestation.form.$this.longLabel"

fun String.formatNumberIfNeeded(numberFormat: NumberFormat): String {
    return try {
        if (this.contains("%")) {
            val cleanVersion = this.removeSuffix("%")
            val value = cleanVersion.toFloat()
            numberFormat.format(value) + "%"
        } else {
            val value = this.toFloat()
            numberFormat.format(value)
        }
    } catch (e: Exception) {
        Timber.v("Current string [$this] can't be formatted")
        this
    }
}