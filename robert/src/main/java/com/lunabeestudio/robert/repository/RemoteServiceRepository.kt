/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.repository

import android.content.Context
import android.util.Base64
import androidx.annotation.WorkerThread
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.domain.model.StatusReport
import com.lunabeestudio.robert.BuildConfig
import com.lunabeestudio.robert.RobertConstant
import com.lunabeestudio.robert.datasource.ConfigurationDataSource
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData

@WorkerThread
internal class RemoteServiceRepository(
    private val remoteServiceDataSource: RemoteServiceDataSource,
    private val sharedCryptoDataSource: SharedCryptoDataSource,
    private val keystoreDataSource: LocalKeystoreDataSource,
    private val configurationDataSource: ConfigurationDataSource
) {

    suspend fun generateCaptcha(apiVersion: String, type: String, local: String): RobertResultData<String> =
        remoteServiceDataSource.generateCaptcha(apiVersion, type, local)

    suspend fun getCaptchaImage(apiVersion: String, captchaId: String, path: String): RobertResult =
        remoteServiceDataSource.getCaptcha(apiVersion, captchaId, "image", path)

    suspend fun getCaptchaAudio(apiVersion: String, captchaId: String, path: String): RobertResult =
        remoteServiceDataSource.getCaptcha(apiVersion, captchaId, "audio", path)

    suspend fun registerV2(apiVersion: String, captcha: String, captchaId: String): RobertResultData<RegisterReport> {
        val keyPair = sharedCryptoDataSource.createECDHKeyPair()

        val publicKey64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val registerResult = remoteServiceDataSource.registerV2(apiVersion, captcha, captchaId, publicKey64)

        if (registerResult is RobertResultData.Success) {
            sharedCryptoDataSource.getEncryptionKeys(
                rawServerPublicKey = Base64.decode(BuildConfig.SERVER_PUBLIC_KEY, Base64.NO_WRAP),
                rawLocalPrivateKey = keyPair.private.encoded,
                kADerivation = RobertConstant.KA_STRING_INPUT.toByteArray(),
                kEADerivation = RobertConstant.KEA_STRING_INPUT.toByteArray()).let {
                keystoreDataSource.kA = it.first
                keystoreDataSource.kEA = it.second
            }
        }

        return registerResult
    }

    suspend fun unregister(apiVersion: String, serverStatusUpdate: ServerStatusUpdate): RobertResult =
        remoteServiceDataSource.unregister(apiVersion, serverStatusUpdate)

    suspend fun status(apiVersion: String, serverStatusUpdate: ServerStatusUpdate): RobertResultData<StatusReport> =
        remoteServiceDataSource.status(apiVersion, serverStatusUpdate)

    suspend fun report(apiVersion: String, token: String, localProximityList: List<LocalProximity>): RobertResult =
        remoteServiceDataSource.report(apiVersion, token, localProximityList)

    suspend fun deleteExposureHistory(apiVersion: String, serverStatusUpdate: ServerStatusUpdate): RobertResult =
        remoteServiceDataSource.deleteExposureHistory(apiVersion, serverStatusUpdate)

    suspend fun fetchConfig(context: Context): RobertResultData<List<Configuration>?> =
        configurationDataSource.fetchConfig(context)
}
