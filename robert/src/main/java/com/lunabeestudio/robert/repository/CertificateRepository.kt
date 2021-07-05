/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/23/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.repository

import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.RemoteCertificateDataSource
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.UnknownException
import timber.log.Timber

class CertificateRepository(
    private val remoteDataSource: RemoteCertificateDataSource,
    private val localKeystoreDataSource: LocalKeystoreDataSource,
) {
    suspend fun convertCertificate(
        certificate: RawWalletCertificate,
        to: WalletCertificateType.Format
    ): RobertResultData<String> {
        val robertResultData = localKeystoreDataSource.configuration?.certificateConversionUrl?.let { backendUrl ->
            remoteDataSource.convertCertificate(
                url = backendUrl,
                encodedCertificate = certificate.value,
                from = certificate.type.format,
                to = to
            )
        } ?: RobertResultData.Failure(UnknownException("No configuration"))

        (robertResultData as? RobertResultData.Failure)?.error?.let { exception ->
            Timber.e(exception)
        }

        return robertResultData
    }
}