/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/2/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.extension.isEligibleForSmartWallet
import com.lunabeestudio.stopcovid.extension.recoveryDateOfFirstPositiveTest
import com.lunabeestudio.stopcovid.extension.smartWalletProfileId
import com.lunabeestudio.stopcovid.extension.vaccineDate
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.SmartWalletMap
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.Date

class GetSmartWalletMapUseCase(
    private val walletRepository: WalletRepository,
    private val blacklistDCCManager: BlacklistDCCManager,
    private val getSmartWalletStateUseCase: GetSmartWalletStateUseCase,
) {
    operator fun invoke(nowDate: Date = Date()): Flow<SmartWalletMap> {
        return walletRepository.walletCertificateFlow.filterNotNull().map { walletCertificates ->
            // Group certificates by firstName+birthdate (to avoid twins)
            var groupedCertificates = walletCertificates
                .data
                ?.filterIsInstance<EuropeanCertificate>()
                ?.groupBy { certificate ->
                    certificate.smartWalletProfileId()
                }.orEmpty()
            groupedCertificates = groupedCertificates.filter { (_, certificates) ->
                certificates.isNotEmpty()
            }
            val result = mutableMapOf<String, EuropeanCertificate>()
            groupedCertificates.forEach { (key, certificates) ->
                // Keep only DCC complete and DCC recovery, remove expired and blacklisted
                val mappedCertificates = certificates
                    .filter { certificate -> certificate.isEligibleForSmartWallet(blacklistDCCManager) }
                    .associateWith { getSmartWalletStateUseCase(it, nowDate) }

                val hasNoElgNoExp = mappedCertificates.any {
                    it.value.eligibleDate == null && it.value.smartWalletValidity?.end == null
                }
                if (hasNoElgNoExp) {
                    // Already has non-expirable certificate
                    return@forEach
                }

                val isEligibleToNextDose = mappedCertificates
                    .filter { it.key.type == WalletCertificateType.VACCINATION_EUROPE }
                    .maxByOrNull {
                        it.key.greenCertificate.vaccineDate ?: Date(0)
                    }?.let {
                        it.value.eligibleDate != null
                    }

                if (isEligibleToNextDose == true) {
                    mappedCertificates.maxByOrNull {
                        it.key.greenCertificate.vaccineDate
                            ?: it.key.greenCertificate.recoveryDateOfFirstPositiveTest
                            ?: Date(0)
                    }?.let {
                        // Keep last certificate
                        result[key] = it.key
                    }
                }
            }

            // return latest complete DCC vaccin or DCC recovery by profile
            result
        }
    }
}
