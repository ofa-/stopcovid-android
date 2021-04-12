/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/30/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.extension

import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.WalletCertificateType
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Configuration.walletOldCertificateThresholdInMs(type: WalletCertificateType): Long {
    return TimeUnit.DAYS.toMillis((walletOldCertificateThresholdInDays[type.code.toLowerCase(Locale.getDefault())])?.toLong() ?: 0.toLong())
}

fun Configuration.walletPublicKey(authority: String, certificateId: String): String? {
    return walletPublicKeys.firstOrNull {
        it.auth == authority
    }?.pubKeys?.get(certificateId)
}

fun Configuration.certificateValidityThresholdInMs(): Long {
    return TimeUnit.HOURS.toMillis(testCertificateValidityThresholdInHours.toLong())
}
