/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/05/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

import com.lunabeestudio.domain.model.smartwallet.SmartWalletValidity
import java.util.Date

sealed class SmartWalletState(val smartWalletValidity: SmartWalletValidity?, val eligibleDate: Date?) {

    class Valid(smartWalletValidity: SmartWalletValidity?, eligibilityDate: Date?) : SmartWalletState(smartWalletValidity, eligibilityDate)
    class EligibleSoon(smartWalletValidity: SmartWalletValidity?, eligibilityDate: Date?) : SmartWalletState(
        smartWalletValidity,
        eligibilityDate
    )

    class Eligible(smartWalletValidity: SmartWalletValidity?, eligibilityDate: Date?) : SmartWalletState(
        smartWalletValidity,
        eligibilityDate
    )

    class ExpireSoon(smartWalletValidity: SmartWalletValidity?, eligibilityDate: Date?) : SmartWalletState(
        smartWalletValidity,
        eligibilityDate
    )

    class Expired(smartWalletValidity: SmartWalletValidity?, eligibilityDate: Date?) : SmartWalletState(
        smartWalletValidity,
        eligibilityDate
    )
}