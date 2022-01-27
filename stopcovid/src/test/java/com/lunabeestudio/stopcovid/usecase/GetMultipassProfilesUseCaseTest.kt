/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/19 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.extension.fullName
import com.lunabeestudio.stopcovid.extension.multipassProfileId
import com.lunabeestudio.stopcovid.extension.testResultIsNegative
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.MultipassProfile
import com.lunabeestudio.stopcovid.repository.WalletRepository
import dgca.verifier.app.decoder.model.GreenCertificate
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals

class GetMultipassProfilesUseCaseTest {
    private lateinit var useCase: GetMultipassProfilesUseCase

    private val walletRepository = mockk<WalletRepository>(relaxed = true)

    @Before
    fun init() {
        useCase = GetMultipassProfilesUseCase(
            walletRepository,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun main_case_test() {
        mockkStatic(EuropeanCertificate::multipassProfileId)
        mockkStatic(GreenCertificate::testResultIsNegative)

        val vaccine1A = mockk<EuropeanCertificate>(relaxed = true)
        val vaccine1B = mockk<EuropeanCertificate>(relaxed = true)
        val test1 = mockk<EuropeanCertificate>(relaxed = true)
        val test2 = mockk<EuropeanCertificate>(relaxed = true)

        every { vaccine1A.fullName() } returns "aaa bbb"
        every { vaccine1A.multipassProfileId() } returns "profile1"
        every { vaccine1A.type } returns WalletCertificateType.VACCINATION_EUROPE

        every { vaccine1B.fullName() } returns "aaa bbb"
        every { vaccine1B.multipassProfileId() } returns "profile1"
        every { vaccine1B.type } returns WalletCertificateType.VACCINATION_EUROPE

        every { test1.fullName() } returns "aaa bbb"
        every { test1.multipassProfileId() } returns "profile1"
        every { test1.type } returns WalletCertificateType.SANITARY_EUROPE
        every { test1.greenCertificate.testResultIsNegative } returns true

        every { test2.fullName() } returns "ccc ddd"
        every { test2.multipassProfileId() } returns "profile2"
        every { test2.type } returns WalletCertificateType.SANITARY_EUROPE
        every { test2.greenCertificate.testResultIsNegative } returns true

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(
            TacResult.Success(
                listOf(
                    vaccine1A,
                    vaccine1B,
                    test1,
                    test2
                )
            )
        )

        val expectedProfiles = listOf(
            MultipassProfile("profile1", "Aaa Bbb", listOf(vaccine1A, vaccine1B, test1)),
            MultipassProfile("profile2", "Ccc Ddd", listOf(test2)),
        )

        assertContentEquals(expectedProfiles, useCase.invoke())
    }
}