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

class GetCloseMultipassProfilesUseCase {
    operator fun invoke(profiles: List<MultipassProfile>): List<MultipassProfile> {
        return profiles.filter { currentProfile ->
            profiles
                .filterNot { currentProfile == it } // exclude current profiles
                .any { otherProfile ->
                    val current = currentProfile.certificates.firstOrNull() ?: return@any false
                    val other = otherProfile.certificates.firstOrNull() ?: return@any false

                    current.rawBirthDate() == other.rawBirthDate() &&
                        (current.firstName != other.firstName || current.name != other.name)
                }
        }
    }
}