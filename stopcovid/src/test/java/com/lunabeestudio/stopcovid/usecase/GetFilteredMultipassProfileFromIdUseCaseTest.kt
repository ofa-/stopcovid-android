/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/25 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.isBlacklisted
import com.lunabeestudio.stopcovid.extension.multipassProfileId
import com.lunabeestudio.stopcovid.extension.testResultIsNegative
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.MultipassProfile
import com.lunabeestudio.stopcovid.repository.WalletRepository
import dgca.verifier.app.decoder.model.GreenCertificate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class GetFilteredMultipassProfileFromIdUseCaseTest {
    private lateinit var useCase: GetFilteredMultipassProfileFromIdUseCase

    private val walletRepository = mockk<WalletRepository>(relaxed = true)
    private val robertManager = mockk<RobertManager>(relaxed = true)
    private val blacklistDCCManager = mockk<BlacklistDCCManager>(relaxed = true)

    @Before
    fun init() {
        useCase = GetFilteredMultipassProfileFromIdUseCase(
            robertManager,
            walletRepository,
            blacklistDCCManager,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun vaccine_test_valid_test() {
        mockkStatic(EuropeanCertificate::isBlacklisted)
        mockkStatic(EuropeanCertificate::multipassProfileId)
        mockkStatic(GreenCertificate::testResultIsNegative)

        val vaccine1 = mockk<EuropeanCertificate>(relaxed = true)
        val vaccine2 = mockk<EuropeanCertificate>(relaxed = true)
        val test = mockk<EuropeanCertificate>(relaxed = true)

        val currentTime = System.currentTimeMillis()

        every { robertManager.configuration.multipassConfig?.testMaxDuration } returns 10.hours

        every { vaccine1.multipassProfileId() } returns "profile"
        every { vaccine1.sha256 } returns "vaccine1"
        every { vaccine1.timestamp } returns currentTime - 3.hours.inWholeMilliseconds
        every { vaccine1.type } returns WalletCertificateType.VACCINATION_EUROPE
        coEvery { vaccine1.isBlacklisted(any()) } returns false

        every { vaccine2.multipassProfileId() } returns "profile"
        every { vaccine2.sha256 } returns "vaccine2"
        every { vaccine2.timestamp } returns currentTime - 6.hours.inWholeMilliseconds
        every { vaccine2.type } returns WalletCertificateType.VACCINATION_EUROPE
        coEvery { vaccine2.isBlacklisted(any()) } returns false

        every { test.multipassProfileId() } returns "profile"
        every { test.sha256 } returns "test"
        every { test.timestamp } returns currentTime - 1.hours.inWholeMilliseconds
        every { test.type } returns WalletCertificateType.SANITARY_EUROPE
        every { test.greenCertificate.testResultIsNegative } returns true
        coEvery { test.isBlacklisted(any()) } returns false

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(vaccine1, vaccine2, test)))

        val expectedCertificates = MultipassProfile("profile", "", listOf(test, vaccine1, vaccine2))

        runTest {
            val actualCertificates = useCase.invoke(vaccine1.multipassProfileId())
            assertEquals(expectedCertificates, actualCertificates)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun dup_test() {
        mockkStatic(EuropeanCertificate::isBlacklisted)
        mockkStatic(EuropeanCertificate::multipassProfileId)
        mockkStatic(GreenCertificate::testResultIsNegative)

        val vaccine1 = mockk<EuropeanCertificate>(relaxed = true)
        val vaccine2 = mockk<EuropeanCertificate>(relaxed = true)

        every { vaccine1.multipassProfileId() } returns "profile"
        every { vaccine1.sha256 }.returnsMany("dup", "not_dup")
        every { vaccine1.type } returns WalletCertificateType.VACCINATION_EUROPE
        coEvery { vaccine1.isBlacklisted(any()) } returns false

        every { vaccine2.multipassProfileId() } returns "profile"
        every { vaccine2.sha256 } returns "dup"
        every { vaccine2.type } returns WalletCertificateType.VACCINATION_EUROPE
        coEvery { vaccine2.isBlacklisted(any()) } returns false

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(vaccine1, vaccine2)))

        val expectedProfilesDup = MultipassProfile("profile", "", listOf(vaccine1))
        val expectedProfilesNotDup = MultipassProfile("profile", "", listOf(vaccine1, vaccine2))

        runTest {
            assertEquals(expectedProfilesDup, useCase.invoke("profile"))
            assertEquals(expectedProfilesNotDup, useCase.invoke("profile"))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun exp_test_test() {
        mockkStatic(EuropeanCertificate::isBlacklisted)
        mockkStatic(EuropeanCertificate::multipassProfileId)
        mockkStatic(GreenCertificate::testResultIsNegative)

        val test = mockk<EuropeanCertificate>(relaxed = true)

        val currentTime = System.currentTimeMillis()

        every { robertManager.configuration.multipassConfig?.testMaxDuration }.returnsMany(1.hours, 10.hours)

        every { test.multipassProfileId() } returns "profile"
        every { test.timestamp } returns currentTime - 3.hours.inWholeMilliseconds
        every { test.type } returns WalletCertificateType.SANITARY_EUROPE
        every { test.greenCertificate.testResultIsNegative } returns true
        coEvery { test.isBlacklisted(any()) } returns false

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(test)))

        val expectedProfiles1 = MultipassProfile("profile", "", emptyList())
        val expectedProfiles10 = MultipassProfile("profile", "", listOf(test))

        runTest {
            assertEquals(expectedProfiles1, useCase.invoke("profile"))
            assertEquals(expectedProfiles10, useCase.invoke("profile"))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun blacklisted_test() {
        mockkStatic(EuropeanCertificate::isBlacklisted)
        mockkStatic(EuropeanCertificate::multipassProfileId)
        mockkStatic(GreenCertificate::testResultIsNegative)

        val test = mockk<EuropeanCertificate>(relaxed = true)

        val currentTime = System.currentTimeMillis()

        every { robertManager.configuration.multipassConfig?.testMaxDuration } returns 10.hours

        every { test.multipassProfileId() } returns "profile"
        every { test.timestamp } returns currentTime - 1.hours.inWholeMilliseconds
        every { test.type } returns WalletCertificateType.SANITARY_EUROPE
        every { test.greenCertificate.testResultIsNegative } returns true
        coEvery { test.isBlacklisted(any()) }.returnsMany(true, false)

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(test)))

        val expectedProfilesTrue = MultipassProfile("profile", "", emptyList())
        val expectedProfilesFalse = MultipassProfile("profile", "", listOf(test))

        runTest {
            assertEquals(expectedProfilesTrue, useCase.invoke("profile"))
            assertEquals(expectedProfilesFalse, useCase.invoke("profile"))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun dccLight_test() {
        mockkStatic(EuropeanCertificate::isBlacklisted)
        mockkStatic(EuropeanCertificate::multipassProfileId)
        mockkStatic(GreenCertificate::testResultIsNegative)

        val certificate = mockk<EuropeanCertificate>(relaxed = true)

        every { robertManager.configuration.multipassConfig?.testMaxDuration } returns 10.hours

        every { certificate.multipassProfileId() } returns "profile"
        every { certificate.type }.returnsMany(WalletCertificateType.DCC_LIGHT, WalletCertificateType.VACCINATION_EUROPE)
        coEvery { certificate.isBlacklisted(any()) } returns false

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(certificate)))

        val expectedProfilesDccLight = MultipassProfile("profile", "", emptyList())
        val expectedProfilesVaccine = MultipassProfile("profile", "", listOf(certificate))

        runTest {
            assertEquals(expectedProfilesDccLight, useCase.invoke("profile"))
            assertEquals(expectedProfilesVaccine, useCase.invoke("profile"))
        }
    }
}