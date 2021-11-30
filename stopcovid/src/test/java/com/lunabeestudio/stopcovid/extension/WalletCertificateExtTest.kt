/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/30/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import androidx.lifecycle.MutableLiveData
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.hours

class WalletCertificateExtTest {

    private lateinit var blacklistDCCManager: BlacklistDCCManager

    private val blackListedSha = "blacklisted"

    @Before
    fun init() {
        blacklistDCCManager = mockk(relaxed = true)
        every { blacklistDCCManager.blacklistedDCCHashes } returns MutableLiveData(listOf(blackListedSha))
    }

    @Test
    fun isEligibleForActivityPass_ok() {
        val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
        every { europeanCertificate.sha256 } returns ""
        every { europeanCertificate.type } returns WalletCertificateType.VACCINATION_EUROPE
        every { europeanCertificate.timestamp } returns System.currentTimeMillis()
        every { europeanCertificate.expirationTime } returns Long.MAX_VALUE
        every { europeanCertificate.canRenewActivityPass } returns null

        assert(europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, Int.MAX_VALUE))
    }

    @Test
    fun isEligibleForActivityPass_blacklisted() {
        val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
        every { europeanCertificate.sha256 } returns blackListedSha
        every { europeanCertificate.type } returns WalletCertificateType.VACCINATION_EUROPE
        every { europeanCertificate.expirationTime } returns Long.MAX_VALUE
        every { europeanCertificate.canRenewActivityPass } returns null

        assert(!europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, Int.MAX_VALUE))
    }

    @Test
    fun isEligibleForActivityPass_expired_sanitary() {
        val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
        every { europeanCertificate.sha256 } returns ""
        every { europeanCertificate.type } returns WalletCertificateType.SANITARY_EUROPE
        every { europeanCertificate.timestamp } returns System.currentTimeMillis() - 2.hours.inWholeMilliseconds
        every { europeanCertificate.expirationTime } returns Long.MAX_VALUE
        every { europeanCertificate.canRenewActivityPass } returns null

        assert(!europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, 1))
    }

    @Test
    fun isEligibleForActivityPass_is_expired() {
        val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
        every { europeanCertificate.sha256 } returns ""
        every { europeanCertificate.type } returns WalletCertificateType.VACCINATION_EUROPE
        every { europeanCertificate.expirationTime } returns Long.MIN_VALUE
        every { europeanCertificate.canRenewActivityPass } returns null

        assert(!europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, Int.MAX_VALUE))
    }

    @Test
    fun isEligibleForActivityPass_already_completed() {
        val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
        every { europeanCertificate.sha256 } returns ""
        every { europeanCertificate.type } returns WalletCertificateType.VACCINATION_EUROPE
        every { europeanCertificate.timestamp } returns System.currentTimeMillis()
        every { europeanCertificate.expirationTime } returns Long.MAX_VALUE
        every { europeanCertificate.canRenewActivityPass } returns false

        assert(!europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, Int.MAX_VALUE))
    }
}