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

import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException

class VerifyAndGetCertificateCodeValueUseCase(
    private val verifyCertificateUseCase: VerifyCertificateUseCase,
) {
    suspend operator fun invoke(codeValue: String, certificateFormat: WalletCertificateType.Format?): WalletCertificate {
        val walletCertificate = WalletCertificate.createCertificateFromValue(codeValue)

        if (walletCertificate == null ||
            (certificateFormat != null && walletCertificate.type.format != certificateFormat) ||
            walletCertificate.type == WalletCertificateType.ACTIVITY_PASS
        ) {
            throw WalletCertificateMalformedException()
        }

        walletCertificate.parse()

        verifyCertificateUseCase(walletCertificate)

        return walletCertificate
    }
}