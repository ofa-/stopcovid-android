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
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import timber.log.Timber
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern

fun String.openInExternalBrowser(context: Context) {
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(this))
    val activityInfo: ActivityInfo? = browserIntent.resolveActivityInfo(context.packageManager, browserIntent.flags)
    if (activityInfo?.exported == true) {
        context.startActivity(browserIntent)
    } else {
        Timber.e("No activity to open url")
        Toast.makeText(context, "Unable to open url", Toast.LENGTH_SHORT).show()
    }
}

fun String.startEmailIntent(context: Context) {
    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", this, null))
    try {
        context.startActivity(emailIntent)
    } catch (e: Exception) {
        Timber.e(e)
        Toast.makeText(context, "Unable to send email", Toast.LENGTH_SHORT).show()
    }
}

fun String.isReportCodeValid(): Boolean {
    val uuidCodeRegex = "^[A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{12}$"
    val shortCodeRegex = "^[A-Za-z0-9]{6}$"
    return Pattern.matches(shortCodeRegex, this) || Pattern.matches(uuidCodeRegex, this)
}

fun String.isValidUUID(): Boolean {
    val uuidCodeRegex = "^[A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{12}$"
    return Pattern.matches(uuidCodeRegex, this)
}

fun String.isPostalCode(): Boolean {
    return length == 5 && this.isDigitsOnly()
}

fun String.attestationLabelFromKey(): String = "attestation.form.$this.label"

fun String.attestationPlaceholderFromKey(): String = "attestation.form.$this.placeholder"

fun String.attestationShortLabelFromKey(): String = "attestation.form.$this.shortLabel"

fun String.attestationLongLabelFromKey(): String = "attestation.form.$this.longLabel"

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

@SuppressWarnings("DefaultLocale")
fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.capitalize(Locale.getDefault()) }

fun String.sha256() = MessageDigest
    .getInstance("SHA-256")
    .digest(this.toByteArray())
    .fold("", { str, it -> str + "%02x".format(it) })
