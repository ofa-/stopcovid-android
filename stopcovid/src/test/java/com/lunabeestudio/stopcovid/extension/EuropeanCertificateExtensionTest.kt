/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/21 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.SmartWalletAges
import com.lunabeestudio.domain.model.SmartWalletElg
import com.lunabeestudio.domain.model.SmartWalletExp
import com.lunabeestudio.domain.model.SmartWalletVacc
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.model.Eligible
import com.lunabeestudio.stopcovid.model.EligibleSoon
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.ExpireSoon
import com.lunabeestudio.stopcovid.model.Expired
import com.lunabeestudio.stopcovid.model.Valid
import dgca.verifier.app.decoder.model.GreenCertificate
import dgca.verifier.app.decoder.model.Vaccination
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.days

@Suppress("SpellCheckingInspection")
class EuropeanCertificateExtensionTest {

    private val configuration = mockk<Configuration>(relaxed = true)

    @Before
    fun init() {
        MockKAnnotations.init(this)

        every { configuration.smartWalletAges } returns SmartWalletAges(
            lowElg = 12,
            low = 18,
            lowExpDays = 152,
            high = 65,
        )
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

        every { configuration.smartWalletElg } returns SmartWalletElg(
            vacc22DosesNbDays = 92,
            vaccJan11DosesNbDays = 31,
            recNbDays = 92,
            vacc22DosesNbDaysLow = 184,
            vaccJan11DosesNbDaysLow = 31,
            recNbDaysLow = 184,
            displayElgDays = 26,
            vaccJan22DosesNbDays = 31,
            vaccJan22DosesNbDaysLow = 31,
        )
        every { configuration.smartWalletVacc } returns SmartWalletVacc(
            ar = listOf("EU/1/20/1528", "EU/1/20/1507"),
            ja = listOf("EU/1/20/1525"),
            az = listOf(
                "EU/1/21/1529",
                "Covidshield",
                "Covid-19-recombinant",
                "R-COVI",
            ),
        )
    }

    @After
    internal fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun test_dcc_smart_wallet_state_winter() {
        mockkStatic(::midnightDate)
        // Fri Dec 03 00:00:00 GMT+01:00 2021
        every { midnightDate() } returns Date(1638486000000L)

        val eligibleSoonState = vaccineDcc("2021-09-04").smartWalletState(configuration)
        // Tue Feb 15 00:00:00 GMT+01:00 2022 (pivot3)
        assertEquals(1644879600000L, eligibleSoonState.expirationDate?.time, "eligibleSoon expirationDate is wrong")
        // Sun Dec 05 00:00:00 GMT+01:00 2021
        assertEquals(1638658800000L, eligibleSoonState.eligibleDate?.time, "eligibleSoon eligibilityDate is wrong")
        assertIs<EligibleSoon>(eligibleSoonState)

        val eligibleState = vaccineDcc("2021-07-04").smartWalletState(configuration)
        // Tue Feb 01 00:00:00 GMT+01:00 2022
        assertEquals(1643670000000L, eligibleState.expirationDate?.time, "eligible expirationDate is wrong")
        // Fri Dec 03 00:00:00 GMT+01:00 2021
        assertEquals(1633302000000L, eligibleState.eligibleDate?.time, "eligible eligibilityDate is wrong")
        assertIs<Eligible>(eligibleState)

        val validState = vaccineDcc("2021-11-05").smartWalletState(configuration)
        // Mon Mar 07 00:00:00 GMT+01:00 2022
        assertEquals(1646607600000L, validState.expirationDate?.time, "okVaccineDcc expirationDate is wrong")
        // Sat Feb 05 00:00:00 GMT+01:00 2022
        assertEquals(1644015600000L, validState.eligibleDate?.time, "okVaccineDcc eligibilityDate is wrong")
        assertIs<Valid>(validState)

        val expiredState = vaccineDcc("2021-05-05").smartWalletState(configuration)
        // Fri Dec 03 00:00:00 GMT+01:00 2021
        assertEquals(1638486000000L, expiredState.expirationDate?.time, "expiredVaccineDcc expirationDate is wrong")
        // Thu Aug 05 01:00:00 GMT+02:00 2021
        assertEquals(1628118000000L, expiredState.eligibleDate?.time, "expiredVaccineDcc eligibilityDate is wrong")
        assertIs<Expired>(expiredState)

        val expireSoonState = vaccineDcc("2021-05-06").smartWalletState(configuration)
        // Sat Dec 04 00:00:00 GMT+01:00 2021
        assertEquals(1638572400000L, expireSoonState.expirationDate?.time, "expireSoonVaccineDcc expirationDate is wrong")
        // Fri Aug 06 01:00:00 GMT+02:00 2021
        assertEquals(1628204400000L, expireSoonState.eligibleDate?.time, "expireSoonVaccineDcc eligibilityDate is wrong")
        assertIs<ExpireSoon>(expireSoonState)
    }

    @Test
    fun test_dcc_smart_wallet_state_summer() {
        mockkStatic(::midnightDate)
        // Sun Jul 03 12:00:00 GMT+02:00 2022
        every { midnightDate() } returns Date(1656842400000L)
        val eligibleSoonState = vaccineDcc("2022-04-10").smartWalletState(configuration)
        // Wed Aug 10 00:00:00 GMT+02:00 2022
        assertEquals(1660082400000L, eligibleSoonState.expirationDate?.time, "eligibleSoon expirationDate is wrong")
        // Mon Jul 11 00:00:00 GMT+02:00 2022
        assertEquals(1657490400000L, eligibleSoonState.eligibleDate?.time, "eligibleSoon eligibilityDate is wrong")
        assertIs<EligibleSoon>(eligibleSoonState)

        val eligibleState = vaccineDcc("2022-03-30").smartWalletState(configuration)
        // Fri Jul 29 00:00:00 GMT+02:00 2022
        assertEquals(1659132000000L, eligibleState.expirationDate?.time, "eligible expirationDate is wrong")
        // Wed Jun 29 00:00:00 GMT+02:00 2022
        assertEquals(1656540000000L, eligibleState.eligibleDate?.time, "eligible eligibilityDate is wrong")
        assertIs<Eligible>(eligibleState)

        val validState = vaccineDcc("2022-05-01").smartWalletState(configuration)
        // Tue Aug 30 00:00:00 GMT+02:00 2022
        assertEquals(1661896800000L, validState.expirationDate?.time, "okVaccineDcc expirationDate is wrong")
        // Sun Jul 31 00:00:00 GMT+02:00 2022
        assertEquals(1659304800000L, validState.eligibleDate?.time, "okVaccineDcc eligibilityDate is wrong")
        assertIs<Valid>(validState)

        val expiredState = vaccineDcc("2021-12-03").smartWalletState(configuration)
        // Sun Apr 03 00:00:00 GMT+02:00 2022
        assertEquals(1649023200000L, expiredState.expirationDate?.time, "expiredVaccineDcc expirationDate is wrong")
        // Fri Mar 04 00:00:00 GMT+02:00 2022
        assertEquals(1646431200000L, expiredState.eligibleDate?.time, "expiredVaccineDcc eligibilityDate is wrong")
        assertIs<Expired>(expiredState)

        val expireSoonState = vaccineDcc("2022-03-04").smartWalletState(configuration)
        // Sun Jul 03 00:00:00 GMT+02:00 2022
        assertEquals(1656885600000L, expireSoonState.expirationDate?.time, "expireSoonVaccineDcc expirationDate is wrong")
        // Fri Jun 03 00:00:00 GMT+02:00 2022
        assertEquals(1654293600000L, expireSoonState.eligibleDate?.time, "expireSoonVaccineDcc eligibilityDate is wrong")
        assertIs<ExpireSoon>(expireSoonState)
    }

    @Test
    fun expired_pivot3() {
        mockkStatic(::midnightDate)
        // Fri Feb 18 12:00:00 GMT+01:00 2022
        every { midnightDate() } returns Date(1645138800000L)

        val expiredStatePivot3 = vaccineDcc("2021-09-05").smartWalletState(configuration)
        // Tue Feb 15 00:00:00 GMT+01:00 2022
        assertEquals(1644879600000L, expiredStatePivot3.expirationDate?.time, "expiredStatePivot3 expirationDate is wrong")
        assertIs<Expired>(expiredStatePivot3)
    }

    @Test
    fun expiration_recovery_after_pivot3_more_18years() {
        mockkStatic(GreenCertificate::isRecoveryOrTestPositive)

        val now = Calendar.getInstance().apply { set(2025, 0, 1, 0, 0, 0) }.time

        val recoDateExp = now.time - configuration.smartWalletExp!!.recNbNewDays.days.inWholeMilliseconds - 1.days.inWholeMilliseconds
        val dccExp = recoveryDcc(Date(recoDateExp), "2000-01-01")

        val recoDateValid = now.time - configuration.smartWalletExp!!.recNbNewDays.days.inWholeMilliseconds + 1.days.inWholeMilliseconds
        val dccValid = recoveryDcc(Date(recoDateValid), "2000-01-01")

        assert(dccExp.smartWalletState(configuration).expirationDate!! < now)
        assert(dccValid.smartWalletState(configuration).expirationDate!! > now)
    }

    @Test
    fun expiration_recovery_after_pivot3_less_18years() {
        mockkStatic(GreenCertificate::isRecoveryOrTestPositive)
        val recNbNewDaysLow =
            (configuration.recoveryValidityThreshold!!.min + configuration.recoveryValidityThreshold!!.max).inWholeMilliseconds

        val now = Calendar.getInstance().apply { set(2025, 0, 1, 0, 0, 0) }.time

        val recoDateExp = now.time - recNbNewDaysLow - 1.days.inWholeMilliseconds
        val dccExp = recoveryDcc(Date(recoDateExp), "2022-01-01")

        val recoDateValid = now.time - recNbNewDaysLow + 1.days.inWholeMilliseconds
        val dccValid = recoveryDcc(Date(recoDateValid), "2022-01-01")

        assert(dccExp.smartWalletState(configuration).expirationDate!! < now)
        assert(dccValid.smartWalletState(configuration).expirationDate!! > now)
    }

    @Test
    fun expiration_recovery_before_pivot3() {
        mockkStatic(GreenCertificate::isRecoveryOrTestPositive)

        // 2021-02-01
        val now = Calendar.getInstance().apply { set(2021, 1, 1, 0, 0, 0) }.time

        val recoDateExp = now.time - configuration.smartWalletExp!!.recNbDays.days.inWholeMilliseconds - 1.days.inWholeMilliseconds
        val dccExp = recoveryDcc(Date(recoDateExp), "2000-01-01")

        val recoDateValid = now.time - configuration.smartWalletExp!!.recNbDays.days.inWholeMilliseconds + 1.days.inWholeMilliseconds
        val dccValid = recoveryDcc(Date(recoDateValid), "2000-01-01")

        assert(dccExp.smartWalletState(configuration).expirationDate!! < now)
        assert(dccValid.smartWalletState(configuration).expirationDate!! > now)
    }

    @Test
    fun expiration_recovery_around_pivot3() {
        mockkStatic(GreenCertificate::isRecoveryOrTestPositive)
        val calendar = Calendar.getInstance()

        // 2021-10-01
        val recoDateExpAtPivot3 = calendar.apply { set(2021, 9, 1, 0, 0, 0) }.time
        val dccExpAtPivot3 = recoveryDcc(recoDateExpAtPivot3, "2000-01-01")

        // 2022-02-01
        val recoDateExpAfterPivot3 = calendar.apply { set(2022, 1, 1, 0, 0, 0) }.time
        val dccExpAfterPivot3 = recoveryDcc(recoDateExpAfterPivot3, "2000-01-01")

        // 1644879600000L -> Pivot3
        assertEquals(Date(1644879600000L), dccExpAtPivot3.smartWalletState(configuration).expirationDate!!)
        assertEquals(
            Date(recoDateExpAfterPivot3.time + configuration.smartWalletExp!!.recNbNewDays.days.inWholeMilliseconds),
            dccExpAfterPivot3.smartWalletState(configuration).expirationDate!!,
        )
    }

    private fun vaccineDcc(date: String): EuropeanCertificate {
        val vaccination = mockk<Vaccination>(relaxed = true).apply {
            every { dateOfVaccination } returns date
            every { medicinalProduct } returns "EU/1/20/1528"
            every { doseNumber } returns 2
            every { totalSeriesOfDoses } returns 2
        }
        return mockk<EuropeanCertificate>(relaxed = true).apply {
            every { type } returns WalletCertificateType.VACCINATION_EUROPE
            every { greenCertificate.dateOfBirth } returns "1992-06-12"
            every { greenCertificate.vaccinations } returns listOf(vaccination)
        }
    }

    private fun recoveryDcc(testDate: Date, birthdate: String): EuropeanCertificate {
        return mockk<EuropeanCertificate>(relaxed = true).apply {
            every { greenCertificate.isRecoveryOrTestPositive } returns true
            every { greenCertificate.recoveryDateOfFirstPositiveTestForceTimeZone } returns testDate
            every { greenCertificate.dateOfBirth } returns birthdate
        }
    }
}
