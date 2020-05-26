/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
fun Context.startEmailIntent(emailAddress: String,
    subject: String? = null,
    body: String? = null) {
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

fun Context.showPermissionRationale(strings: Map<String, String>,
    messageKey: String,
    positiveKey: String,
    positiveAction: () -> Unit) {
    MaterialAlertDialogBuilder(this)
        .setTitle(strings["common.permissionsNeeded"])
        .setMessage(strings[messageKey])
        .setPositiveButton(strings[positiveKey]) { _, _ ->
            positiveAction()
        }
        .setNegativeButton(strings["common.cancel"], null)
        .show()
}