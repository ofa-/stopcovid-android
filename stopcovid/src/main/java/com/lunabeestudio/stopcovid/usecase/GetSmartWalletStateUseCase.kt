/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/4 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.future
import com.lunabeestudio.stopcovid.extension.midnightDate
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.SmartWalletState
import java.util.Date

class GetSmartWalletStateUseCase(
    private val computeDccValidityUseCase: ComputeDccValidityUseCase,
    private val computeDccEligibilityUseCase: ComputeDccEligibilityUseCase,
    private val robertManager: RobertManager,
) {
    operator fun invoke(dcc: EuropeanCertificate, nowDate: Date = Date()): SmartWalletState {
        val configuration = robertManager.configuration

        val validity = computeDccValidityUseCase(dcc, nowDate)
        val eligibleDate = computeDccEligibilityUseCase(dcc, nowDate)

        val limitExpireSoonDisplay = configuration.smartWalletEngine?.displayExp
        val limitExpireSoonDate = Date(midnightDate().time + (limitExpireSoonDisplay?.inWholeMilliseconds ?: 0L))
        val limitEligibleSoonDisplay = configuration.smartWalletEngine?.displayElg
        val limitEligibleSoonDate = Date(midnightDate().time + (limitEligibleSoonDisplay?.inWholeMilliseconds ?: 0))
        val expirationDate = validity?.end
        return when {
            expirationDate?.future() == false && eligibleDate != null -> SmartWalletState.Expired(validity, eligibleDate)
            expirationDate?.future() == true &&
                eligibleDate != null &&
                expirationDate.before(limitExpireSoonDate) -> SmartWalletState.ExpireSoon(
                validity,
                eligibleDate
            )
            eligibleDate?.future() == false -> SmartWalletState.Eligible(validity, eligibleDate)
            eligibleDate?.future() == true && eligibleDate.before(limitEligibleSoonDate) -> SmartWalletState.EligibleSoon(
                validity,
                eligibleDate
            )
            else -> SmartWalletState.Valid(validity, eligibleDate)
        }
    }
}