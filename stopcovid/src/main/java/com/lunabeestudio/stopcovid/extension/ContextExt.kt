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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.`interface`.IsolationApplication
import com.lunabeestudio.stopcovid.manager.IsolationManager

fun Context.robertManager(): RobertManager = (applicationContext as RobertApplication).robertManager

fun Context.isolationManager(): IsolationManager = (applicationContext as IsolationApplication).isolationManager

fun Context.secureKeystoreDataSource(): SecureKeystoreDataSource = (applicationContext as StopCovid).secureKeystoreDataSource

fun Context.showExpiredCodeAlert(strings: Map<String, String>) {
    MaterialAlertDialogBuilder(this)
        .setTitle(strings["enterCodeController.alert.expiredCode.title"])
        .setMessage(strings["enterCodeController.alert.expiredCode.message"])
        .setPositiveButton(strings["common.ok"], null)
        .show()
}

fun Context.showInvalidCodeAlert(strings: Map<String, String>) {
    MaterialAlertDialogBuilder(this)
        .setTitle(strings["enterCodeController.alert.invalidCode.title"])
        .setMessage(strings["enterCodeController.alert.invalidCode.message"])
        .setPositiveButton(strings["common.ok"], null)
        .show()
}

/**
 * Start an explicit intent with text to share
 *
 * @param text The text to share
 */
fun Context.startTextIntent(text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}