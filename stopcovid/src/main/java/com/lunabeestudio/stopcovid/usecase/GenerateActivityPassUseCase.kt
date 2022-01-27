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

import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.UnknownException
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.GenerateActivityPassException
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateNoKeyException
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.UUID

class GenerateActivityPassUseCase(
    private val walletRepository: WalletRepository,
    private val verifyCertificateUseCase: VerifyCertificateUseCase,
) {
    operator fun invoke(certificate: EuropeanCertificate): Flow<TacResult<GenerateActivityPassState>> {
        return generateActivityPass(certificate.raw)
    }

    operator fun invoke(certificateId: String): Flow<TacResult<GenerateActivityPassState>> {
        return flow {
            val rawRootDcc = walletRepository.getCertificateById(certificateId)?.raw
            if (rawRootDcc != null) {
                emitAll(generateActivityPass(rawRootDcc))
            } else {
                emit(TacResult.Failure(UnknownException("No certificate found with id $certificateId")))
            }
        }
    }

    private fun generateActivityPass(certificate: RawWalletCertificate): Flow<TacResult<GenerateActivityPassState>> {
        return flow {
            val dccResult = walletRepository.generateActivityPass(certificate.value)

            when (dccResult) {
                is RobertResultData.Failure -> emit(TacResult.Failure(dccResult.error.toCovidException()))
                is RobertResultData.Success -> {
                    emit(TacResult.Loading(GenerateActivityPassState.Downloaded))

                    val dccData = dccResult.data

                    try {
                        dccData.rawDcc.values.firstOrNull()?.let {
                            EuropeanCertificate.getCertificate(it)?.let { dcc ->
                                dcc.parse()
                                verifyCertificateUseCase(dcc)
                            }
                        }
                    } catch (e: WalletCertificateNoKeyException) {
                        emit(
                            TacResult.Failure(
                                GenerateActivityPassException(
                                    message = "Unable to validate activity pass generated",
                                    cause = e,
                                )
                            )
                        )
                        return@flow
                    }

                    // Set canRenewDccLight to true in case the worker is killed before the end
                    certificate.canRenewDccLight = true
                    walletRepository.updateCertificate(certificate)

                    var isFirst = true
                    dccData.rawDcc.forEach {
                        val rawDcc = RawWalletCertificate(
                            id = UUID.randomUUID().toString(),
                            type = WalletCertificateType.DCC_LIGHT,
                            value = it.value,
                            timestamp = -1, // not needed to store dcc light
                            isFavorite = false,
                            expireAt = it.key,
                            rootWalletCertificateId = certificate.id,
                            null,
                        )
                        walletRepository.saveCertificate(rawDcc)

                        if (isFirst) {
                            emit(
                                TacResult.Loading(
                                    GenerateActivityPassState.FirstActivityPassSaved(
                                        WalletCertificate.createCertificateFromRaw(
                                            rawDcc
                                        ) as? EuropeanCertificate
                                    )
                                )
                            )
                            isFirst = false
                        }
                    }

                    // Now that we saved every activity pass, store the real value of dccData.completed
                    certificate.canRenewDccLight = !dccData.completed
                    walletRepository.updateCertificate(certificate)

                    emit(TacResult.Success(GenerateActivityPassState.Ended))
                }
            }
        }.onStart { emit(TacResult.Loading()) }
    }
}

enum class GenerateActivityPassStateName { DOWNLOADED, FIRST_ACTIVITY_PASS_SAVED, ENDED }

sealed class GenerateActivityPassState(val name: GenerateActivityPassStateName) {
    object Downloaded : GenerateActivityPassState(GenerateActivityPassStateName.DOWNLOADED)
    class FirstActivityPassSaved(val activityPass: EuropeanCertificate?) :
        GenerateActivityPassState(GenerateActivityPassStateName.FIRST_ACTIVITY_PASS_SAVED)

    object Ended : GenerateActivityPassState(GenerateActivityPassStateName.ENDED)
}
