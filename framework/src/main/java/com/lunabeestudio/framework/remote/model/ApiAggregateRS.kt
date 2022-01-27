/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/23 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.model

internal class ApiAggregateRS(
    val response: String,
)

internal class ApiAggregateCertificate(
    val certificate: String,
)

internal class ApiAggregateError(
    val error: String,
    val errors: List<Error>,
    val message: String,
    val path: String,
    val status: Int,
    val timestamp: String
) {
    class Error(
        val code: String,
        val field: String,
        val message: String
    )
}