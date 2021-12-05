/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/6/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.isBlacklisted
import com.lunabeestudio.stopcovid.extension.isEligibleForActivityPass
import com.lunabeestudio.stopcovid.manager.DccCertificatesManager
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.TacResult
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

class CleanAndRenewActivityPassUseCase(
    private val walletRepository: WalletRepository,
    private val blacklistDCCManager: BlacklistDCCManager,
    private val dccCertificatesManager: DccCertificatesManager,
    private val robertManager: RobertManager,
    private val generateActivityPassUseCase: GenerateActivityPassUseCase,
) {
    suspend operator fun invoke() {
        val dccList: List<EuropeanCertificate> = walletRepository.walletCertificateFlow
            .filterNotNull()
            .first()
            .filterIsInstance<EuropeanCertificate>()

        val dccToClear = dccList.filter {
            it.isBlacklisted(blacklistDCCManager)
        }

        // Clear blacklisted DCC
        dccToClear.forEach {
            walletRepository.deleteAllActivityPassForCertificate(it.id)
        }

        // Revoke and renew activity passes with old kid
        walletRepository.getAllActivityPassDistinctByRootId().forEach { walletCertificate ->
            val certificateKeyIds = dccCertificatesManager.certificates.keys
            if (!certificateKeyIds.contains(walletCertificate.keyCertificateId)) {
                walletCertificate.rootWalletCertificateId?.let { rootId ->
                    walletRepository.deleteAllActivityPassForCertificate(rootId)
                    generateActivityPassUseCase(rootId).collect()
                }
            }
        }

        val renewableCertificates = dccList.filter {
            it.canRenewActivityPass == true &&
                it.isEligibleForActivityPass(
                    blacklistDCCManager,
                    robertManager.configuration
                )
        }

        renewableCertificates.forEach { certificate ->
            val validActivityPassCount = walletRepository.countValidActivityPassForCertificate(
                certificate.id,
                System.currentTimeMillis()
            )
            // Auto renew activity pass if threshold is reached
            if (validActivityPassCount <= robertManager.configuration.renewThreshold) {
                val activityPassToRemove = walletRepository.getAllActivityPassForRootId(certificate.id)
                generateActivityPassUseCase(certificate).collect { result ->
                    if (result is TacResult.Success) {
                        walletRepository.deleteActivityPass(*activityPassToRemove.map { it.id }.toTypedArray())
                    }
                }
            }
        }

        // Remove expired activity pass
        walletRepository.deleteAllExpiredActivityPass(System.currentTimeMillis())
    }
}