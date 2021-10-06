/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/14/04 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.proxy

interface AnalyticsInfosProvider {
    fun getBaseUrl(): String
    fun getApiVersion(): String
    fun getAppVersion(): String
    fun getAppBuild(): Int
    suspend fun getPlacesCount(): Int
    suspend fun getFormsCount(): Int
    suspend fun getCertificatesCount(): Int
    fun userHaveAZipCode(): Boolean
    fun getDateSample(): Long?
    fun getDateFirstSymptom(): Long?
    fun getDateLastContactNotification(): Long?
}