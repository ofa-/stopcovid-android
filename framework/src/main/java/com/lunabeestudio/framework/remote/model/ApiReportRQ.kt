/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.remote.model

import com.lunabeestudio.domain.model.LocalProximity

internal class ApiReportRQ(
    val token: String,
    val contacts: List<ApiGroupedHellosReportRQ>?
) {
    companion object {
        fun fromLocalProximityList(token: String, localProximityList: List<LocalProximity>): ApiReportRQ {
            val contacts = localProximityList.groupBy { it.eccBase64 + it.ebidBase64 }.map { entry ->
                val apiDistinctiveHelloInfoList = entry.value.map { (_, _, mac, helloTime, collectedTime, rawRssi, calibratedRssi) ->
                    ApiDistinctiveHelloInfoWithinEpochForSameEBIDRQ(collectedTime,
                        helloTime,
                        mac,
                        rawRssi,
                        calibratedRssi)
                }
                ApiGroupedHellosReportRQ(entry.value.first().ebidBase64, entry.value.first().eccBase64, apiDistinctiveHelloInfoList)
            }

            return ApiReportRQ(token, contacts)
        }
    }
}
