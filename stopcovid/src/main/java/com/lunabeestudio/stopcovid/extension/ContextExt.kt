/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager

fun Context.robertManager(): RobertManager = (applicationContext as RobertApplication).robertManager

fun Context.showInvalidCodeAlert(strings: Map<String, String>) {
    MaterialAlertDialogBuilder(this)
        .setTitle(strings["enterCodeController.alert.invalidCode.title"])
        .setMessage(strings["enterCodeController.alert.invalidCode.message"])
        .setPositiveButton(strings["common.ok"], null)
        .show()
}