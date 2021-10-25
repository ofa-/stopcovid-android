/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/6/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.domain.extension.walletPublicKey
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.manager.DccCertificatesManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateNoKeyException
import com.lunabeestudio.stopcovid.model.getForKeyId

class VerifyCertificateUseCase(
    private val dccCertificatesManager: DccCertificatesManager,
    private val robertManager: RobertManager,
) {
    operator fun invoke(walletCertificate: WalletCertificate) {
        val key: String? = when (walletCertificate) {
            is EuropeanCertificate -> dccCertificatesManager.certificates.getForKeyId(walletCertificate.keyCertificateId)
            is FrenchCertificate -> robertManager.configuration.walletPublicKey(
                walletCertificate.keyAuthority,
                walletCertificate.keyCertificateId
            )
        }

        if (key != null) {
            walletCertificate.verifyKey(key)
        } else if ((walletCertificate as? EuropeanCertificate)?.greenCertificate?.isFrench == true
            || walletCertificate !is EuropeanCertificate
        ) {
            // Only check French certificates
            throw WalletCertificateNoKeyException()
        }
    }
}