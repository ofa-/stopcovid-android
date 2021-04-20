/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/02/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.model

class HealthInfos(
    type: Int,
    os: String,
    deviceModel: String,
    osVersion: String,
    appVersion: String,
    appBuild: Int,
    receivedHelloMessagesCount: Int,
    secondsTracingActivated: Long,
    placesCount: Int,
    val riskLevel: Float?,
    val dateSample: String?,
    val dateFirstSymptoms: String?,
    val dateLastContactNotification: String?,
) : Infos(type, os, deviceModel, osVersion, appVersion, appBuild, receivedHelloMessagesCount, secondsTracingActivated, placesCount)