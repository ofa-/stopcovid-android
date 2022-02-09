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

import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.SmartWalletExp
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.hours

class WalletCertificateExtTest {

    private lateinit var blacklistDCCManager: BlacklistDCCManager
    private lateinit var configuration: Configuration

    private val blackListedSha = "blacklisted"

    @Before
    fun init() {
        MockKAnnotations.init(this)
        blacklistDCCManager = mockk(relaxed = true)
        configuration = mockk(relaxed = true)
        every { configuration.smartWalletExp?.pivot1 } returns "2021-12-15"
        every { configuration.smartWalletExp?.pivot2 } returns "2022-01-15"
        every { configuration.smartWalletExp?.pivot3 } returns "2022-02-15"
        coEvery { blacklistDCCManager.isBlacklisted(blackListedSha) } returns true
    }

    @Test
    fun isEligibleForActivityPass_ok() {
        runBlocking {
            val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
            every { europeanCertificate.sha256 } returns ""
            every { europeanCertificate.type } returns WalletCertificateType.VACCINATION_EUROPE
            every { europeanCertificate.timestamp } returns System.currentTimeMillis()
            every { europeanCertificate.expirationTime } returns Long.MAX_VALUE
            every { europeanCertificate.canRenewActivityPass } returns null
            every { configuration.activityPassSkipNegTestHours } returns Int.MAX_VALUE
            every { configuration.smartWalletExp } returns SmartWalletExp(
                pivot1 = "2020-12-15T00:01:00+0200",
                pivot2 = "2021-01-15T00:01:00+0200",
                pivot3 = "2022-02-15T00:01:00+0200",
                vacc22DosesNbDays = 212,
                vacc11DosesNbDays = 219,
                vacc22DosesNbNewDays = 122,
                vacc11DosesNbNewDays = 122,
                recNbDays = 182,
                vaccJan11DosesNbDays = 67,
                displayExpOnAllDcc = 240,
                displayExpDays = 21,
                vaccJan22DosesNbDays = 214,
                vaccJan22DosesNbNewDays = 122,
                recNbNewDays = 122,
            )

            assert(europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, configuration))
        }
    }

    @Test
    fun isEligibleForActivityPass_blacklisted() {
        runBlocking {
            val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
            every { europeanCertificate.sha256 } returns blackListedSha
            every { europeanCertificate.type } returns WalletCertificateType.VACCINATION_EUROPE
            every { europeanCertificate.expirationTime } returns Long.MAX_VALUE
            every { europeanCertificate.canRenewActivityPass } returns null
            every { configuration.activityPassSkipNegTestHours } returns Int.MAX_VALUE

            assert(!europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, configuration))
        }
    }

    @Test
    fun isEligibleForActivityPass_expired_sanitary() {
        runBlocking {
            val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
            every { europeanCertificate.sha256 } returns ""
            every { europeanCertificate.type } returns WalletCertificateType.SANITARY_EUROPE
            every { europeanCertificate.timestamp } returns System.currentTimeMillis() - 2.hours.inWholeMilliseconds
            every { europeanCertificate.expirationTime } returns Long.MAX_VALUE
            every { europeanCertificate.canRenewActivityPass } returns null
            every { configuration.activityPassSkipNegTestHours } returns 1

            assert(!europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, configuration))
        }
    }

    @Test
    fun isEligibleForActivityPass_is_signature_expired() {
        runBlocking {
            val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
            every { europeanCertificate.sha256 } returns ""
            every { europeanCertificate.type } returns WalletCertificateType.VACCINATION_EUROPE
            every { europeanCertificate.expirationTime } returns Long.MIN_VALUE
            every { europeanCertificate.canRenewActivityPass } returns null
            every { configuration.activityPassSkipNegTestHours } returns Int.MAX_VALUE

            assert(!europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, configuration))
        }
    }

    @Test
    fun isEligibleForActivityPass_already_completed() {
        runBlocking {
            val europeanCertificate = mockk<EuropeanCertificate>(relaxed = true)
            every { europeanCertificate.sha256 } returns ""
            every { europeanCertificate.type } returns WalletCertificateType.VACCINATION_EUROPE
            every { europeanCertificate.timestamp } returns System.currentTimeMillis()
            every { europeanCertificate.expirationTime } returns Long.MAX_VALUE
            every { europeanCertificate.canRenewActivityPass } returns false
            every { configuration.activityPassSkipNegTestHours } returns Int.MAX_VALUE

            assert(!europeanCertificate.isEligibleForActivityPass(blacklistDCCManager, configuration))
        }
    }
}