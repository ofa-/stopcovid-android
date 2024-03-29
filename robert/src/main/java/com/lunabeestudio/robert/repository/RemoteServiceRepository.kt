/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.repository

import android.content.Context
import android.util.Base64
import androidx.annotation.WorkerThread
import com.lunabeestudio.domain.model.Calibration
import com.lunabeestudio.domain.model.CaptchaType
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.ReportResponse
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.domain.model.StatusReport
import com.lunabeestudio.robert.RobertConstant
import com.lunabeestudio.robert.datasource.RobertCalibrationDataSource
import com.lunabeestudio.robert.datasource.RobertConfigurationDataSource
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData

@WorkerThread
internal class RemoteServiceRepository(
    private val remoteServiceDataSource: RemoteServiceDataSource,
    private val sharedCryptoDataSource: SharedCryptoDataSource,
    private val keystoreDataSource: LocalKeystoreDataSource,
    private val configurationDataSource: RobertConfigurationDataSource,
    private val calibrationDataSource: RobertCalibrationDataSource,
    private val serverPublicKey: String,
) {

    suspend fun generateCaptcha(apiVersion: String, type: CaptchaType, local: String): RobertResultData<String> =
        remoteServiceDataSource.generateCaptcha(apiVersion, type, local)

    suspend fun getCaptchaImage(apiVersion: String, captchaId: String, path: String): RobertResult =
        remoteServiceDataSource.getCaptcha(apiVersion, captchaId, CaptchaType.IMAGE, path)

    suspend fun getCaptchaAudio(apiVersion: String, captchaId: String, path: String): RobertResult =
        remoteServiceDataSource.getCaptcha(apiVersion, captchaId, CaptchaType.AUDIO, path)

    suspend fun registerV2(apiVersion: String, captcha: String, captchaId: String): RobertResultData<RegisterReport> {
        val keyPair = sharedCryptoDataSource.createECDHKeyPair()

        val publicKey64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val registerResult = remoteServiceDataSource.registerV2(apiVersion, captcha, captchaId, publicKey64)

        return if (registerResult is RobertResultData.Success) {
            try {
                sharedCryptoDataSource.getEncryptionKeys(
                    rawServerPublicKey = Base64.decode(serverPublicKey, Base64.NO_WRAP),
                    rawLocalPrivateKey = keyPair.private.encoded,
                    derivationDataArray = listOf(
                        RobertConstant.KA_STRING_INPUT.toByteArray(),
                        RobertConstant.KEA_STRING_INPUT.toByteArray()
                    )
                ).let {
                    keystoreDataSource.isRegistered = true
                    keystoreDataSource.kA = it.getOrNull(0)
                    keystoreDataSource.kEA = it.getOrNull(1)
                }
                registerResult
            } catch (e: RobertException) {
                RobertResultData.Failure(e)
            }
        } else {
            registerResult
        }
    }

    suspend fun unregister(apiVersion: String, serverStatusUpdate: ServerStatusUpdate): RobertResult =
        remoteServiceDataSource.unregister(apiVersion, serverStatusUpdate)

    suspend fun status(apiVersion: String, serverStatusUpdate: ServerStatusUpdate): RobertResultData<StatusReport> =
        remoteServiceDataSource.status(apiVersion, serverStatusUpdate)

    suspend fun report(
        apiVersion: String,
        token: String,
        localProximityList: List<LocalProximity>,
        onProgressUpdate: (Float) -> Unit,
    ): RobertResultData<ReportResponse> =
        remoteServiceDataSource.report(apiVersion, token, localProximityList, onProgressUpdate)

    suspend fun deleteExposureHistory(apiVersion: String, serverStatusUpdate: ServerStatusUpdate): RobertResult =
        remoteServiceDataSource.deleteExposureHistory(apiVersion, serverStatusUpdate)

    suspend fun fetchOrLoadConfig(context: Context): RobertResultData<Configuration> =
        configurationDataSource.fetchOrLoadConfig(context)

    fun loadConfig(context: Context): Configuration =
        configurationDataSource.loadConfig(context)

    suspend fun fetchOrLoadCalibration(context: Context): RobertResultData<Calibration> =
        calibrationDataSource.fetchOrLoadCalibration(context)

    fun loadCalibration(context: Context): Calibration =
        calibrationDataSource.loadCalibration(context)
}
