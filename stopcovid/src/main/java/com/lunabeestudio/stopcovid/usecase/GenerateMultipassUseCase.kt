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

import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.UnknownException
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class GenerateMultipassUseCase(
    private val walletRepository: WalletRepository,
    private val verifyCertificateUseCase: VerifyCertificateUseCase,
) {
    operator fun invoke(certificateList: List<EuropeanCertificate>): Flow<TacResult<EuropeanCertificate>> {
        return flow {
            val dccResult = walletRepository.generateMultipass(certificateList.map(WalletCertificate::value))

            when (dccResult) {
                is RobertResultData.Failure -> emit(TacResult.Failure(dccResult.error))
                is RobertResultData.Success -> {
                    val dccValue = dccResult.data

                    val multipass = try {
                        val forcedType = WalletCertificateType.MULTI_PASS.takeIf {
                            EuropeanCertificate.getTypeFromValue(dccValue) == WalletCertificateType.DCC_LIGHT
                        }
                        EuropeanCertificate.getCertificate(dccValue, forcedType = forcedType)?.also { dcc ->
                            dcc.parse()
                            verifyCertificateUseCase(dcc)
                        }
                    } catch (e: Exception) {
                        emit(TacResult.Failure(e))
                        return@flow
                    }

                    if (multipass != null) {
                        walletRepository.saveCertificate(multipass)
                        emit(TacResult.Success(multipass))
                    } else {
                        emit(TacResult.Failure(UnknownException("Generated multipass is null")))
                    }
                }
            }
        }.onStart { emit(TacResult.Loading()) }
    }
}
