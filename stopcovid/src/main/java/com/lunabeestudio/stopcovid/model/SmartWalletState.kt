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

import java.util.Date

sealed class SmartWalletState(val expirationDate: Date?, val eligibleDate: Date?)

class Valid(expirationDate: Date?, eligibilityDate: Date?) : SmartWalletState(expirationDate, eligibilityDate)
class EligibleSoon(expirationDate: Date?, eligibilityDate: Date?) : SmartWalletState(expirationDate, eligibilityDate)
class Eligible(expirationDate: Date?, eligibilityDate: Date?) : SmartWalletState(expirationDate, eligibilityDate)
class ExpireSoon(expirationDate: Date?, eligibilityDate: Date?) : SmartWalletState(expirationDate, eligibilityDate)
class Expired(expirationDate: Date?, eligibilityDate: Date?) : SmartWalletState(expirationDate, eligibilityDate)