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

import com.lunabeestudio.stopcovid.extension.capitalizeWords
import com.lunabeestudio.stopcovid.extension.fullName
import com.lunabeestudio.stopcovid.extension.multipassProfileId
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.MultipassProfile
import com.lunabeestudio.stopcovid.repository.WalletRepository

class GetMultipassProfilesUseCase(
    private val walletRepository: WalletRepository,
) {
    operator fun invoke(): List<MultipassProfile> {
        return walletRepository.walletCertificateFlow.value.data
            ?.filterIsInstance<EuropeanCertificate>()
            ?.map {
                it.multipassProfileId()
            }
            ?.distinct()
            ?.mapNotNull(::toProfile)
            ?: emptyList()
    }

    private fun toProfile(profileId: String): MultipassProfile? {
        val certificates = walletRepository.walletCertificateFlow.value.data.orEmpty()
            .asSequence()
            .filterIsInstance<EuropeanCertificate>()
            .filter { dcc -> dcc.multipassProfileId() == profileId }
            .toList()
        return certificates.firstOrNull()
            ?.fullName()
            ?.let { fullname -> MultipassProfile(profileId, fullname.capitalizeWords(), certificates) }
    }
}