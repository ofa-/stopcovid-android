/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.datasource

import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResultData

interface RemoteCertificateDataSource {
    suspend fun convertCertificateV1(
        encodedCertificate: String,
        from: WalletCertificateType.Format,
        to: WalletCertificateType.Format
    ): RobertResultData<String>

    suspend fun convertCertificateV2(
        robertManager: RobertManager,
        encodedCertificate: String,
        from: WalletCertificateType.Format,
        to: WalletCertificateType.Format
    ): RobertResultData<String>
}