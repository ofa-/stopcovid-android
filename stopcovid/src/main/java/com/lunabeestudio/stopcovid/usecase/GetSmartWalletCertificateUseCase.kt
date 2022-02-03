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

import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.isEligibleForSmartWallet
import com.lunabeestudio.stopcovid.extension.parseOrNull
import com.lunabeestudio.stopcovid.extension.recoveryDateOfFirstPositiveTest
import com.lunabeestudio.stopcovid.extension.smartWalletProfileId
import com.lunabeestudio.stopcovid.extension.testDateTimeOfCollection
import com.lunabeestudio.stopcovid.extension.vaccineDate
import com.lunabeestudio.stopcovid.extension.yearMonthDayUsParser
import com.lunabeestudio.stopcovid.extension.yearsOld
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.SmartWallet
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class GetSmartWalletCertificateUseCase(
    private val walletRepository: WalletRepository,
    private val blacklistDCCManager: BlacklistDCCManager,
    private val robertManager: RobertManager,
) {
    private val yearMonthDayUsParser = yearMonthDayUsParser()

    operator fun invoke(): Flow<SmartWallet> = walletRepository.walletCertificateFlow.filterNotNull().map { walletCertificates ->
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
            // Skip too young
            val age = yearMonthDayUsParser.parseOrNull(certificates.first().greenCertificate.dateOfBirth)?.yearsOld() ?: 0
            val minAge = robertManager.configuration.smartWalletAges?.lowElg ?: 0
            if (age >= minAge) {
                // Keep only DCC complete and DCC recovery, remove expired and blacklisted
                val filteredCertificates = certificates.filter { certificate ->
                    certificate.isEligibleForSmartWallet(blacklistDCCManager)
                }
                // Put latest first
                val sortedCertificates = filteredCertificates.sortedByDescending { certificate ->
                    certificate.greenCertificate.vaccineDate
                        ?: certificate.greenCertificate.recoveryDateOfFirstPositiveTest
                        ?: certificate.greenCertificate.testDateTimeOfCollection
                }
                // Only keep latest
                sortedCertificates.firstOrNull()?.let { certificate ->
                    result[key] = certificate
                }
            }
        }
        // return latest complete DCC vaccin or DCC recovery by profile
        result
    }
}
