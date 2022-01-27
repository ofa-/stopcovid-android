/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/24 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.stopcovid.extension.rawBirthDate
import com.lunabeestudio.stopcovid.model.MultipassProfile
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals

class GetCloseMultipassProfilesUseCaseTest {
    private lateinit var useCase: GetCloseMultipassProfilesUseCase

    @Before
    fun init() {
        useCase = GetCloseMultipassProfilesUseCase()
    }

    @Test
    fun main_case_test() {
        val profileA1 = mockk<MultipassProfile>(relaxed = true)
        every { profileA1.id } returns "A1"
        every { profileA1.certificates.firstOrNull()?.firstName } returns "aaa"
        every { profileA1.certificates.firstOrNull()?.name } returns "1"
        every { profileA1.certificates.firstOrNull()?.rawBirthDate() } returns "dob"

        val profileA2 = mockk<MultipassProfile>(relaxed = true)
        every { profileA2.id } returns "A2"
        every { profileA2.certificates.firstOrNull()?.firstName } returns "aaa"
        every { profileA2.certificates.firstOrNull()?.name } returns "2"
        every { profileA2.certificates.firstOrNull()?.rawBirthDate() } returns "dob"

        val profileB1 = mockk<MultipassProfile>(relaxed = true)
        every { profileB1.id } returns "B1"
        every { profileB1.certificates.firstOrNull()?.firstName } returns "bbb"
        every { profileB1.certificates.firstOrNull()?.name } returns "1"
        every { profileB1.certificates.firstOrNull()?.rawBirthDate() } returns "dob2"

        val profileB2 = mockk<MultipassProfile>(relaxed = true)
        every { profileB2.id } returns "B2"
        every { profileB2.certificates.firstOrNull()?.firstName } returns "bbb"
        every { profileB2.certificates.firstOrNull()?.name } returns "1"
        every { profileB2.certificates.firstOrNull()?.rawBirthDate() } returns "dob3"

        val expectedIds = listOf("A1", "A2")
        val actualIds = useCase(listOf(profileA1, profileA2, profileB1, profileB2)).map { it.id }

        assertContentEquals(expectedIds, actualIds)
    }
}