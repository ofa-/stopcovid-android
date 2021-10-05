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
import android.content.DialogInterface
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.InjectionContainer
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.manager.DccCertificatesManager
import com.lunabeestudio.stopcovid.manager.IsolationManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.RisksLevelManager
import com.lunabeestudio.stopcovid.repository.AttestationRepository
import com.lunabeestudio.stopcovid.repository.VenueRepository
import com.lunabeestudio.stopcovid.repository.WalletRepository

private val Context.injectionContainer: InjectionContainer
    get() = (applicationContext as StopCovid).injectionContainer

fun Context.robertManager(): RobertManager = injectionContainer.robertManager
fun Context.stringsManager(): StringsManager = injectionContainer.stringsManager
fun Context.isolationManager(): IsolationManager = injectionContainer.isolationManager
fun Context.keyFiguresManager(): KeyFiguresManager = injectionContainer.keyFiguresManager
fun Context.secureKeystoreDataSource(): SecureKeystoreDataSource = injectionContainer.secureKeystoreDataSource
fun Context.dccCertificatesManager(): DccCertificatesManager = injectionContainer.dccCertificatesManager
fun Context.risksLevelManager(): RisksLevelManager = injectionContainer.risksLevelManager
fun Context.analyticsManager(): AnalyticsManager = injectionContainer.analyticsManager
fun Context.attestationRepository(): AttestationRepository = injectionContainer.attestationRepository
fun Context.venueRepository(): VenueRepository = injectionContainer.venueRepository
fun Context.walletRepository(): WalletRepository = injectionContainer.walletRepository

fun Context.showExpiredCodeAlert(strings: Map<String, String>, listener: DialogInterface.OnDismissListener?) {
    MaterialAlertDialogBuilder(this)
        .setTitle(strings["enterCodeController.alert.expiredCode.title"])
        .setMessage(strings["enterCodeController.alert.expiredCode.message"])
        .setPositiveButton(strings["common.ok"], null)
        .setOnDismissListener(listener)
        .show()
}

fun Context.showInvalidCodeAlert(strings: Map<String, String>, listener: DialogInterface.OnDismissListener?) {
    MaterialAlertDialogBuilder(this)
        .setTitle(strings["enterCodeController.alert.invalidCode.title"])
        .setMessage(strings["enterCodeController.alert.invalidCode.message"])
        .setPositiveButton(strings["common.ok"], null)
        .setOnDismissListener(listener)
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