/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.datasource

import com.lunabeestudio.domain.model.CaptchaType
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.ReportResponse
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.domain.model.StatusReport
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData

interface RemoteServiceDataSource {
    suspend fun generateCaptcha(apiVersion: String, type: CaptchaType, language: String): RobertResultData<String>
    suspend fun getCaptcha(apiVersion: String, captchaId: String, type: CaptchaType, path: String): RobertResult
    suspend fun registerV2(
        apiVersion: String,
        captcha: String,
        captchaId: String,
        clientPublicECDHKey: String,
    ): RobertResultData<RegisterReport>

    suspend fun unregister(apiVersion: String, ssu: ServerStatusUpdate): RobertResult
    suspend fun status(apiVersion: String, ssu: ServerStatusUpdate): RobertResultData<StatusReport>
    suspend fun report(
        apiVersion: String,
        token: String,
        localProximityList: List<LocalProximity>,
        onProgressUpdate: ((Float) -> Unit)? = null
    ): RobertResultData<ReportResponse>

    suspend fun deleteExposureHistory(apiVersion: String, ssu: ServerStatusUpdate): RobertResult
}
