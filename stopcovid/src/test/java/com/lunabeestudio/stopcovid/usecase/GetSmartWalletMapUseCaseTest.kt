/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/11 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.domain.model.smartwallet.SmartWalletValidity
import com.lunabeestudio.stopcovid.extension.isEligibleForSmartWallet
import com.lunabeestudio.stopcovid.extension.recoveryDateOfFirstPositiveTest
import com.lunabeestudio.stopcovid.extension.smartWalletProfileId
import com.lunabeestudio.stopcovid.extension.vaccineDate
import com.lunabeestudio.stopcovid.extension.yearMonthDayUsParser
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.SmartWalletState
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.repository.WalletRepository
import dgca.verifier.app.decoder.model.GreenCertificate
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals

@Suppress("SpellCheckingInspection")
class GetSmartWalletMapUseCaseTest {

    private val yearMonthDayUsParser = yearMonthDayUsParser()

    private lateinit var mapUseCase: GetSmartWalletMapUseCase

    private val walletRepository = mockk<WalletRepository>(relaxed = true)
    private val blacklistDCCManager = mockk<BlacklistDCCManager>(relaxed = true)
    private val getSmartWalletStateUseCase = mockk<GetSmartWalletStateUseCase>(relaxed = true)

    @Before
    fun init() {
        MockKAnnotations.init(this)

        mockkStatic(EuropeanCertificate::smartWalletProfileId)
        mockkStatic(WalletCertificate::isEligibleForSmartWallet)
        mockkStatic(GreenCertificate::recoveryDateOfFirstPositiveTest)

        mapUseCase = GetSmartWalletMapUseCase(
            walletRepository,
            blacklistDCCManager,
            getSmartWalletStateUseCase,
        )

        coEvery { blacklistDCCManager.isBlacklisted(any()) } returns false
    }

    @After
    internal fun tearDown() {
        clearAllMocks()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getSmartWalletStateUseCase_1_vax_eligible() {
        val today = yearMonthDayUsParser.parse("2020-01-01")!!

        val certificate = mockk<EuropeanCertificate>(relaxed = true).also {
            every { it.type } returns WalletCertificateType.VACCINATION_EUROPE
            coEvery { it.isEligibleForSmartWallet(any()) } returns true
        }
        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(
            TacResult.Success(
                listOf(
                    certificate,
                )
            )
        )
        every { getSmartWalletStateUseCase.invoke(certificate, today) } returns SmartWalletState.Valid(
            SmartWalletValidity(null, null),
            Date(),
        )

        runTest {
            assertEquals(certificate, mapUseCase.invoke(today).first().values.first())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getSmartWalletStateUseCase_1_vax_then_1_reco_eligible() {
        val vaxDate = yearMonthDayUsParser.parse("2019-06-01")!!
        val recoDate = yearMonthDayUsParser.parse("2019-11-01")!!
        val recoElgDate = yearMonthDayUsParser.parse("2019-11-15")!!
        val today = yearMonthDayUsParser.parse("2020-01-01")!!
        val vaxElgDate = yearMonthDayUsParser.parse("2020-01-15")!!

        val profileId = "profile_0"

        val vax = mockk<EuropeanCertificate>(relaxed = true).also {
            every { it.type } returns WalletCertificateType.VACCINATION_EUROPE
            every { it.smartWalletProfileId() } returns profileId
            every { it.greenCertificate.vaccineDate } returns vaxDate
            coEvery { it.isEligibleForSmartWallet(any()) } returns true
        }

        val reco = mockk<EuropeanCertificate>(relaxed = true).also {
            every { it.type } returns WalletCertificateType.RECOVERY_EUROPE
            every { it.smartWalletProfileId() } returns profileId
            every { it.greenCertificate.recoveryDateOfFirstPositiveTest } returns recoDate
            coEvery { it.isEligibleForSmartWallet(any()) } returns true
        }

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(vax, reco)))
        every { getSmartWalletStateUseCase.invoke(vax, today) } returns mockk<SmartWalletState>().also {
            every { it.eligibleDate } returns vaxElgDate
            every { it.smartWalletValidity } returns SmartWalletValidity(null, null)
        }

        every { getSmartWalletStateUseCase.invoke(vax, today) } returns mockk<SmartWalletState>().also {
            every { it.eligibleDate } returns recoElgDate
            every { it.smartWalletValidity } returns SmartWalletValidity(null, null)
        }

        runTest {
            assertEquals(mapOf(profileId to reco), mapUseCase.invoke(today).first())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getSmartWalletStateUseCase_1_dcc_no_exp_no_elg() {
        val today = yearMonthDayUsParser.parse("2020-01-01")!!

        val certificate = mockk<EuropeanCertificate>(relaxed = true).also {
            coEvery { it.isEligibleForSmartWallet(any()) } returns true
        }
        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(
            TacResult.Success(listOf(certificate))
        )
        every { getSmartWalletStateUseCase.invoke(certificate, today) } returns mockk<SmartWalletState>().also {
            every { it.eligibleDate } returns null
            every { it.smartWalletValidity } returns SmartWalletValidity(null, null)
        }

        runTest {
            assert(mapUseCase.invoke(today).first().isEmpty())
        }
    }
}
