/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/19 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.capitalizeWords
import com.lunabeestudio.stopcovid.extension.fullName
import com.lunabeestudio.stopcovid.extension.isBlacklisted
import com.lunabeestudio.stopcovid.extension.multipassProfileId
import com.lunabeestudio.stopcovid.extension.testResultIsNegative
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.MultipassProfile
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlin.time.Duration.Companion.milliseconds

class GetFilteredMultipassProfileFromIdUseCase(
    private val robertManager: RobertManager,
    private val walletRepository: WalletRepository,
    private val blacklistDCCManager: BlacklistDCCManager,
) {
    suspend operator fun invoke(profileId: String): MultipassProfile? {
        val multipassConfig = robertManager.configuration.multipassConfig ?: return null
        var displayName = profileId
        val certificates = walletRepository.walletCertificateFlow.value.data.orEmpty()
            .asSequence()
            .filterIsInstance<EuropeanCertificate>()
            .filter { dcc -> dcc.multipassProfileId() == profileId }
            .filterNot { dcc ->
                displayName = dcc.fullName().capitalizeWords() // Extract name asap in case no certificate match
                dcc.type == WalletCertificateType.DCC_LIGHT || dcc.type == WalletCertificateType.MULTI_PASS
            }
            .filterNot { dcc ->
                dcc.greenCertificate.testResultIsNegative == true &&
                    (System.currentTimeMillis() - dcc.timestamp).milliseconds > multipassConfig.testMaxDuration
            }
            .distinctBy { dcc -> dcc.sha256 }
            .sortedByDescending { it.timestamp }
            .toList()
            .filterNot { dcc -> dcc.isBlacklisted(blacklistDCCManager) }
        return MultipassProfile(profileId, displayName, certificates)
    }
}
