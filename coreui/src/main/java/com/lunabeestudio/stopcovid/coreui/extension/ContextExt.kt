/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.coreui.UiConstants
import java.util.Locale

fun Context.isNightMode(): Boolean {
    return when (resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO -> false
        Configuration.UI_MODE_NIGHT_UNDEFINED -> false
        else -> false
    }
}

/**
 * Start an explicit intent to send a email to [emailAddress]
 *
 * @param emailAddress The address to send the email
 * @param subject The subject to use in the email
 * @param body The body to use in the email
 */
fun Context.startEmailIntent(
    emailAddress: String,
    subject: String? = null,
    body: String? = null,
) {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:")
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
    if (subject != null) {
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    if (body != null) {
        intent.putExtra(Intent.EXTRA_TEXT, body)
    }
    startActivity(Intent.createChooser(intent, null))
}

fun Context.showPermissionSettingsDialog(
    strings: Map<String, String>,
    messageKey: String,
    positiveKey: String,
    neutralKey: String?,
    cancelable: Boolean,
    positiveAction: () -> Unit,
    negativeAction: (() -> Unit)?,
    neutralAction: (() -> Unit)?,
) {
    MaterialAlertDialogBuilder(this).apply {
        setTitle(strings["common.permissionsNeeded"])
        setMessage(strings[messageKey])
        setCancelable(cancelable)
        setPositiveButton(strings[positiveKey]) { _, _ ->
            positiveAction()
        }
        if (!neutralKey.isNullOrBlank() && neutralAction != null) {
            setNeutralButton(strings[neutralKey]) { _, _ ->
                neutralAction()
            }
        }
        setNegativeButton(strings["common.cancel"]) { _, _ ->
            negativeAction?.invoke()
        }
        show()
    }
}

fun Context.getApplicationLanguage(): String {
    val userLanguage = PreferenceManager.getDefaultSharedPreferences(this).userLanguage
    val supportedLanguages = UiConstants.SUPPORTED_LOCALES.map { it.language }
    return userLanguage ?: if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        val locales = resources.configuration.locales.toList()
        locales.firstOrNull { supportedLanguages.contains(it.language) }?.language ?: UiConstants.DEFAULT_LANGUAGE
    } else {
        Locale.getDefault().language.takeIf { supportedLanguages.contains(it) } ?: UiConstants.DEFAULT_LANGUAGE
    }
}

fun Context?.getApplicationLocale(): Locale {
    return this?.getApplicationLanguage()?.let {
        Locale(it)
    } ?: Locale.getDefault().takeIf { UiConstants.SUPPORTED_LOCALES.contains(it) } ?: Locale(UiConstants.DEFAULT_LANGUAGE)
}