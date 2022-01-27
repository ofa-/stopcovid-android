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
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import timber.log.Timber
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

fun String.openInExternalBrowser(context: Context, showToast: Boolean = true): Boolean {
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(this))
    val activityInfo: ActivityInfo? = browserIntent.resolveActivityInfo(context.packageManager, browserIntent.flags)
    return if (activityInfo?.exported == true) {
        context.startActivity(browserIntent)
        true
    } else {
        Timber.e("No activity to open url")
        if (showToast) {
            Toast.makeText(context, "Unable to open url", Toast.LENGTH_SHORT).show()
        }
        false
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

fun String.titleCaseFirstChar(): String = replaceFirstChar {
    if (it.isLowerCase()) {
        it.titlecase(Locale.ROOT)
    } else {
        it.toString()
    }
}

fun String.capitalizeWords(): String = lowercase().split(" ").joinToString(" ") { it.titleCaseFirstChar() }

fun String.sha256(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this.toByteArray())
    .fold("", { str, it -> str + "%02x".format(it) })

// Dirty hack to fix strange display of quotes on some Android devices
fun String.fixQuoteInString(): String = replace("'à'", "à")

fun String?.safeParseColor(): Int = Color.parseColor(this ?: "#0B0091")

fun String?.orNA(): String = this ?: "N/A"

// https://stackoverflow.com/a/35849652/10935947
val String.countryCodeToFlagEmoji: String
    get() {
        return try {
            val firstLetter = Character.codePointAt(this.uppercase(), 0) - 0x41 + 0x1F1E6
            val secondLetter = Character.codePointAt(this.uppercase(), 1) - 0x41 + 0x1F1E6
            String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
        } catch (e: Exception) {
            Timber.e(e)
            ""
        }
    }

fun String.splitUrlFragment(): List<String> = this.split(Char(0x1E))

fun String.getLabelKeyFigureFromConfig(): String {
    return "keyfigure.$this"
}

fun String.iOSCommonHash(): Long {
    var h = 1125899906842597L // prime
    val len: Int = length
    for (i in 0 until len) {
        h = 31 * h + this[i].code.toLong()
    }
    return abs(h)
}