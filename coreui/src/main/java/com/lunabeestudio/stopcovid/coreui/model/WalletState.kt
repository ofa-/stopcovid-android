/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/24/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.model

import androidx.annotation.DrawableRes
import com.lunabeestudio.stopcovid.coreui.R

enum class WalletState(
    val theme: CardTheme,
    @DrawableRes val icon: Int,
    val bodyKey: String,
) {
    // /!\ Order is important
    NONE(
        theme = CardTheme.Primary,
        icon = R.drawable.wallet_card,
        bodyKey = "home.attestationSection.sanitaryCertificates.cell.subtitle",
    ),
    ELIGIBLE(
        theme = CardTheme.Eligible,
        icon = R.drawable.wallet_eligible_card,
        bodyKey = "home.attestationSection.sanitaryCertificates.eligible.cell.subtitle",
    ),
    ELIGIBLE_SOON(
        theme = CardTheme.Eligible,
        icon = R.drawable.wallet_eligible_card,
        bodyKey = "home.attestationSection.sanitaryCertificates.eligibleSoon.cell.subtitle",
    ),
    WARNING(
        theme = CardTheme.Warning,
        icon = R.drawable.wallet_warning_card,
        bodyKey = "home.attestationSection.sanitaryCertificates.expiredSoon.cell.subtitle",
    ),
    ALERT(
        theme = CardTheme.Alert,
        icon = R.drawable.wallet_alert_card,
        bodyKey = "home.attestationSection.sanitaryCertificates.expired.cell.subtitle",
    )
}