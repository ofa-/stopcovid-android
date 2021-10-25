/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/27/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.datasource

interface RemoteDccLightDataSource {
    suspend fun generateActivityPass(
        serverPublicKey: String,
        encodedCertificate: String,
    ): com.lunabeestudio.robert.model.RobertResultData<com.lunabeestudio.domain.model.DccLightData>
}