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

fun Context.showPermissionRationale(
    strings: Map<String, String>,
    messageKey: String,
    positiveKey: String,
    cancelable: Boolean,
    positiveAction: () -> Unit,
    negativeAction: (() -> Unit)?,
) {
    MaterialAlertDialogBuilder(this)
        .setTitle(strings["common.permissionsNeeded"])
        .setMessage(strings[messageKey])
        .setCancelable(cancelable)
        .setPositiveButton(strings[positiveKey]) { _, _ ->
            positiveAction()
        }
        .setNegativeButton(strings["common.cancel"]) { _, _ ->
            negativeAction?.invoke()
        }
        .show()
}

fun Context.getFirstSupportedLanguage(): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        val locales = resources.configuration.locales.toList()
        locales.firstOrNull { UiConstants.SUPPORTED_LANGUAGE.contains(it.language) }?.language ?: UiConstants.DEFAULT_LANGUAGE
    } else {
        Locale.getDefault().language.takeIf { UiConstants.SUPPORTED_LANGUAGE.contains(it) } ?: UiConstants.DEFAULT_LANGUAGE
    }
}