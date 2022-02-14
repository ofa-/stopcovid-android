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

import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.isBlacklisted
import com.lunabeestudio.stopcovid.extension.isEligibleForActivityPass
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.manager.DccCertificatesManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.repository.WalletRepository
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class CleanAndRenewActivityPassUseCaseTest {

    private lateinit var useCase: CleanAndRenewActivityPassUseCase

    private val walletRepository = mockk<WalletRepository>(relaxed = true)
    private val blacklistDCCManager = mockk<BlacklistDCCManager>(relaxed = true)
    private val dccCertificatesManager = mockk<DccCertificatesManager>(relaxed = true)
    private val robertManager = mockk<RobertManager>(relaxed = true)
    private val generateActivityPassUseCase = mockk<GenerateActivityPassUseCase>(relaxed = true)
    private val getSmartWalletStateUseCase = mockk<GetSmartWalletStateUseCase>(relaxed = true)

    @Before
    fun init() {
        useCase = CleanAndRenewActivityPassUseCase(
            walletRepository,
            blacklistDCCManager,
            dccCertificatesManager,
            robertManager,
            generateActivityPassUseCase,
            getSmartWalletStateUseCase,
        )

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(emptyList()))
    }

    @Test
    fun blacklist_clean() {
        mockkStatic("com.lunabeestudio.stopcovid.extension.WalletCertificateExtKt")

        val blacklistedDcc = mockk<EuropeanCertificate>(relaxed = true)
        every { blacklistedDcc.id } returns "blacklisted"
        coEvery { blacklistedDcc.isBlacklisted(any()) } returns true

        val notBlacklistedDcc = mockk<EuropeanCertificate>(relaxed = true)
        every { notBlacklistedDcc.id } returns "not_blacklisted"
        coEvery { notBlacklistedDcc.isBlacklisted(any()) } returns false

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(
            TacResult.Success(
                listOf(
                    blacklistedDcc,
                    notBlacklistedDcc,
                )
            )
        )

        runBlocking { useCase.invoke() }

        coVerify(exactly = 1) {
            walletRepository.deleteAllActivityPassForCertificate(any())
        }

        coVerify(exactly = 1) {
            walletRepository.deleteAllActivityPassForCertificate("blacklisted")
        }
    }

    @Test
    fun revoke_kid_and_renew() {
        val revokedDcc = mockk<EuropeanCertificate>(relaxed = true)
        every { revokedDcc.id } returns "revoked"
        every { revokedDcc.keyCertificateId } returns "revoked_kid"
        every { revokedDcc.rootWalletCertificateId } returns "revoked_root_id"

        val notRevokedDcc = mockk<EuropeanCertificate>(relaxed = true)
        every { notRevokedDcc.id } returns "not_revoked"
        every { notRevokedDcc.keyCertificateId } returns "not_revoked_kid"
        every { notRevokedDcc.rootWalletCertificateId } returns "not_revoked_root_id"

        every { dccCertificatesManager.certificates } returns mapOf("not_revoked_kid" to emptyList())
        coEvery { walletRepository.getAllActivityPassDistinctByRootId() } returns listOf(revokedDcc, notRevokedDcc)

        runBlocking { useCase.invoke() }

        coVerify(exactly = 1) {
            walletRepository.deleteAllActivityPassForCertificate(any())
            generateActivityPassUseCase.invoke(any<String>())
        }

        coVerify(exactly = 1) {
            walletRepository.deleteAllActivityPassForCertificate("revoked_root_id")
            generateActivityPassUseCase.invoke("revoked_root_id")
        }
    }

    @Test
    fun renew_threshold_reached() {
        mockkStatic(EuropeanCertificate::isEligibleForActivityPass)
        mockkStatic(EuropeanCertificate::isBlacklisted)

        val renewableDcc = mockk<EuropeanCertificate>(relaxed = true)
        every { renewableDcc.id } returns "renewable"
        every { renewableDcc.canRenewActivityPass } returns true
        coEvery { renewableDcc.isEligibleForActivityPass(any(), any(), any()) } returns true
        coEvery { walletRepository.countValidActivityPassForCertificate("renewable", any()) } returns Int.MIN_VALUE
        coEvery { renewableDcc.isBlacklisted(any()) } returns false

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(renewableDcc)))

        runBlocking { useCase.invoke() }

        coVerify(exactly = 1) {
            walletRepository.countValidActivityPassForCertificate(any(), any())
        }

        coVerify(exactly = 1) {
            walletRepository.countValidActivityPassForCertificate("renewable", any())
        }

        coVerify(exactly = 1) {
            generateActivityPassUseCase.invoke(any<EuropeanCertificate>())
        }

        coVerify(exactly = 1) {
            generateActivityPassUseCase.invoke(renewableDcc)
        }
    }

    @Test
    fun renew_threshold_not_reached() {
        mockkStatic(EuropeanCertificate::isEligibleForActivityPass)
        mockkStatic(EuropeanCertificate::isBlacklisted)

        val thresholdNotReachedDcc = mockk<EuropeanCertificate>(relaxed = true)
        every { thresholdNotReachedDcc.id } returns "threshold_not_reached"
        every { thresholdNotReachedDcc.canRenewActivityPass } returns true
        coEvery { thresholdNotReachedDcc.isEligibleForActivityPass(any(), any(), any()) } returns true
        coEvery { walletRepository.countValidActivityPassForCertificate("threshold_not_reached", any()) } returns Int.MAX_VALUE
        coEvery { thresholdNotReachedDcc.isBlacklisted(any()) } returns false

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(thresholdNotReachedDcc)))

        runBlocking { useCase.invoke() }

        coVerify(exactly = 1) {
            walletRepository.countValidActivityPassForCertificate(any(), any())
        }

        coVerify(exactly = 1) {
            walletRepository.countValidActivityPassForCertificate("threshold_not_reached", any())
        }

        coVerify { generateActivityPassUseCase wasNot called }
    }

    @Test
    fun renew_threshold_reached_not_renewable() {
        mockkStatic(EuropeanCertificate::isEligibleForActivityPass)
        mockkStatic(EuropeanCertificate::isBlacklisted)

        val notRenewableDcc = mockk<EuropeanCertificate>(relaxed = true)
        every { notRenewableDcc.id } returns "not_renewable"
        every { notRenewableDcc.canRenewActivityPass } returns false
        coEvery { notRenewableDcc.isEligibleForActivityPass(any(), any(), any()) } returns true
        coEvery { notRenewableDcc.isBlacklisted(any()) } returns false

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(notRenewableDcc)))

        runBlocking { useCase.invoke() }

        coVerify(exactly = 0) { walletRepository.countValidActivityPassForCertificate(any(), any()) }
        coVerify { generateActivityPassUseCase wasNot called }
    }

    @Test
    fun renew_threshold_reached_not_eligible() {
        mockkStatic(EuropeanCertificate::isEligibleForActivityPass)
        mockkStatic(EuropeanCertificate::isBlacklisted)

        val notEligibleDcc = mockk<EuropeanCertificate>(relaxed = true)
        every { notEligibleDcc.id } returns "not_eligible"
        every { notEligibleDcc.canRenewActivityPass } returns true
        coEvery { notEligibleDcc.isEligibleForActivityPass(any(), any(), any()) } returns false
        coEvery { notEligibleDcc.isBlacklisted(any()) } returns false

        every { walletRepository.walletCertificateFlow } returns MutableStateFlow(TacResult.Success(listOf(notEligibleDcc)))

        runBlocking { useCase.invoke() }

        coVerify(exactly = 0) { walletRepository.countValidActivityPassForCertificate(any(), any()) }
        coVerify { generateActivityPassUseCase wasNot called }
    }
}