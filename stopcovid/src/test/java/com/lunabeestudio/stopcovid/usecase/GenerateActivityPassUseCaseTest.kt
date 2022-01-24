/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/7/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.domain.model.DccLightData
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.Date

class GenerateActivityPassUseCaseTest {
    private lateinit var useCase: GenerateActivityPassUseCase

    private val walletRepository = mockk<WalletRepository>(relaxed = true)
    private val verifyCertificateUseCase = mockk<VerifyCertificateUseCase>(relaxed = true)

    @Before
    fun init() {
        useCase = GenerateActivityPassUseCase(
            walletRepository,
            verifyCertificateUseCase,
        )

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(emptyList()))
    }

    @Test
    fun success_not_completed_flow() {
        success_flow(false)
    }

    @Test
    fun success_completed_flow() {
        success_flow(true)
    }

    private fun success_flow(completed: Boolean) {
        mockkStatic(WalletCertificate::raw)
        mockkObject(EuropeanCertificate.Companion)

        val rootDcc = mockk<EuropeanCertificate>(relaxed = true)
        val rootRawDcc = RawWalletCertificate(
            "root_id",
            WalletCertificateType.VACCINATION_EUROPE,
            "root_value",
            0,
            false,
            null,
            null,
            null,
        )

        every { rootDcc.raw } returns rootRawDcc

        val dccLightData = DccLightData(completed, mapOf(Date(0) to "dcc_raw"))

        coEvery { walletRepository.generateActivityPass("root_value") } returns RobertResultData.Success(dccLightData)

        every {
            EuropeanCertificate.getCertificate(
                "dcc_raw",
                "root_id",
                false,
                null,
                null,
            )
        } returns null // bypass dcc checks

        val resultList = runBlocking {
            useCase(rootDcc).onEach { result ->
                if (result.data is GenerateActivityPassState.FirstActivityPassSaved) {
                    coVerifyOrder {
                        walletRepository.updateCertificate(
                            RawWalletCertificate(
                                "root_id",
                                WalletCertificateType.VACCINATION_EUROPE,
                                "root_value",
                                0,
                                false,
                                null,
                                null,
                                true,
                            )
                        )
                        walletRepository.saveCertificate(
                            *varargAll {
                                it.type == WalletCertificateType.ACTIVITY_PASS &&
                                    it.value == "dcc_raw" &&
                                    it.timestamp == -1L &&
                                    !it.isFavorite &&
                                    it.expireAt == Date(0) &&
                                    it.rootWalletCertificateId == "root_id" &&
                                    it.canRenewDccLight == null
                            }
                        )
                    }
                }
            }.toList()
        }

        assert(resultList[0] is TacResult.Loading && resultList[0].data == null)
        assert(resultList[1] is TacResult.Loading && resultList[1].data is GenerateActivityPassState.Downloaded)
        assert(resultList[2] is TacResult.Loading && resultList[2].data is GenerateActivityPassState.FirstActivityPassSaved)
        assert(resultList[3] is TacResult.Success && resultList[3].data is GenerateActivityPassState.Ended)
        assert(resultList.getOrNull(4) == null)

        coVerify(atLeast = 1) {
            walletRepository.updateCertificate(
                RawWalletCertificate(
                    "root_id",
                    WalletCertificateType.VACCINATION_EUROPE,
                    "root_value",
                    0,
                    false,
                    null,
                    null,
                    !completed,
                )
            )
        }
    }

    @Test
    fun success_completed_empty_dcc_flow() {
        mockkStatic(WalletCertificate::raw)

        val rootDcc = mockk<EuropeanCertificate>(relaxed = true)
        val rootRawDcc = RawWalletCertificate(
            "root_id",
            WalletCertificateType.VACCINATION_EUROPE,
            "root_value",
            0,
            false,
            null,
            null,
            null,
        )

        every { rootDcc.raw } returns rootRawDcc

        val dccLightData = DccLightData(true, emptyMap())

        coEvery { walletRepository.generateActivityPass("root_value") } returns RobertResultData.Success(dccLightData)

        val resultList = runBlocking {
            useCase(rootDcc).toList()
        }

        assert(resultList[0] is TacResult.Loading && resultList[0].data == null)
        assert(resultList[1] is TacResult.Loading && resultList[1].data is GenerateActivityPassState.Downloaded)
        assert(resultList[2] is TacResult.Success && resultList[2].data is GenerateActivityPassState.Ended)
        assert(resultList.getOrNull(3) == null)

        coVerify(atLeast = 1) {
            walletRepository.updateCertificate(
                RawWalletCertificate(
                    "root_id",
                    WalletCertificateType.VACCINATION_EUROPE,
                    "root_value",
                    0,
                    false,
                    null,
                    null,
                    false,
                )
            )
        }

        coVerify(exactly = 0) {
            walletRepository.saveCertificate(*anyVararg())
        }
    }
}