/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/14/04 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.model

open class Infos(
    val type: Int,
    val os: String,
    val deviceModel: String,
    val osVersion: String,
    val appVersion: String,
    val appBuild: Int,
    val receivedHelloMessagesCount: Int,
    val secondsTracingActivated: Long,
    val placesCount: Int,
)