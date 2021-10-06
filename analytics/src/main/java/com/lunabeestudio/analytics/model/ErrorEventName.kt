/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/01/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.model

enum class ErrorEventName {
    ERR_ATTESTATION_DB,
    ERR_ATTESTATION_MIG,
    ERR_VENUES_DB,
    ERR_VENUES_MIG,
    ERR_WALLET_DB,
    ERR_WALLET_MIG,
    ERR_WALLET_MIG_DECRYPT,
    ERR_WALLET_MIG_CONVERT,
    ERR_WALLET_INSERT_DB,
}