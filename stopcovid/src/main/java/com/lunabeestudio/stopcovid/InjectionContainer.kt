/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with context
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/31/8 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid

import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.framework.crypto.BouncyCastleCryptoDataSource
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.local.datasource.SecureFileEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.framework.local.datasource.SecureFileLocalProximityDataSource
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.framework.manager.DebugManager
import com.lunabeestudio.framework.manager.LocalProximityFilterImpl
import com.lunabeestudio.framework.remote.datasource.CleaDataSource
import com.lunabeestudio.framework.remote.datasource.InGroupeDatasource
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.robert.datasource.RobertCalibrationDataSource
import com.lunabeestudio.robert.datasource.RobertConfigurationDataSource
import com.lunabeestudio.stopcovid.coreui.EnvConstant
import com.lunabeestudio.stopcovid.coreui.manager.CalibrationManager
import com.lunabeestudio.stopcovid.coreui.manager.ConfigManager
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.manager.Blacklist2DDOCManager
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.manager.CertificatesDocumentsManager
import com.lunabeestudio.stopcovid.manager.DccCertificatesManager
import com.lunabeestudio.stopcovid.manager.FormManager
import com.lunabeestudio.stopcovid.manager.InfoCenterManager
import com.lunabeestudio.stopcovid.manager.IsolationManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.LinksManager
import com.lunabeestudio.stopcovid.manager.MoreKeyFiguresManager
import com.lunabeestudio.stopcovid.manager.PrivacyManager
import com.lunabeestudio.stopcovid.manager.RisksLevelManager
import com.lunabeestudio.stopcovid.manager.TacCalibrationDataSource
import com.lunabeestudio.stopcovid.manager.TacConfigurationDataSource
import com.lunabeestudio.stopcovid.manager.VaccinationCenterManager
import com.lunabeestudio.stopcovid.repository.AttestationRepository
import com.lunabeestudio.stopcovid.repository.VenueRepository
import com.lunabeestudio.stopcovid.repository.WalletRepository
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class InjectionContainer(private val context: StopCovid, val coroutineScope: CoroutineScope) {

    val serverManager: ServerManager by lazy { ServerManager(context) }
    val stringsManager: StringsManager by lazy { StringsManager(serverManager.okHttpClient) }
    val privacyManager: PrivacyManager by lazy { PrivacyManager(serverManager.okHttpClient) }
    val linksManager: LinksManager by lazy { LinksManager(serverManager.okHttpClient) }
    val moreKeyFiguresManager: MoreKeyFiguresManager by lazy { MoreKeyFiguresManager(serverManager.okHttpClient) }
    val infoCenterManager: InfoCenterManager by lazy { InfoCenterManager(serverManager, stringsManager) }
    val formManager: FormManager by lazy { FormManager(serverManager) }
    val vaccinationCenterManager: VaccinationCenterManager by lazy { VaccinationCenterManager(serverManager) }
    val keyFiguresManager: KeyFiguresManager by lazy { KeyFiguresManager(serverManager) }
    val risksLevelManager: RisksLevelManager by lazy { RisksLevelManager(serverManager) }
    val blacklistDCCManager: BlacklistDCCManager by lazy { BlacklistDCCManager(serverManager) }
    val blacklist2DDOCManager: Blacklist2DDOCManager by lazy { Blacklist2DDOCManager(serverManager) }
    val calibrationManager: CalibrationManager by lazy { CalibrationManager(serverManager.okHttpClient) }
    val configManager: ConfigManager by lazy { ConfigManager(serverManager.okHttpClient) }
    val analyticsManager: AnalyticsManager by lazy { AnalyticsManager(serverManager.okHttpClient, context) }
    val cryptoManager: LocalCryptoManager by lazy { LocalCryptoManager(context) }
    val certificatesDocumentsManager: CertificatesDocumentsManager by lazy { CertificatesDocumentsManager(serverManager) }
    val dccCertificatesManager: DccCertificatesManager by lazy { DccCertificatesManager(serverManager) }
    val calibrationDataSource: RobertCalibrationDataSource by lazy { TacCalibrationDataSource(calibrationManager) }
    val configurationDataSource: RobertConfigurationDataSource by lazy { TacConfigurationDataSource(configManager) }
    val cryptoDataSource: BouncyCastleCryptoDataSource = BouncyCastleCryptoDataSource()
    val secureKeystoreDataSource: SecureKeystoreDataSource by lazy { SecureKeystoreDataSource(context, cryptoManager, ConcurrentHashMap()) }
    val logsDir: File by lazy { File(context.filesDir, Constants.Logs.DIR_NAME) }
    val debugManager: DebugManager by lazy { DebugManager(context, secureKeystoreDataSource, logsDir, cryptoManager) }
    val attestationRepository: AttestationRepository by lazy { AttestationRepository(secureKeystoreDataSource) }
    val venueRepository: VenueRepository by lazy { VenueRepository(secureKeystoreDataSource) }
    val walletRepository: WalletRepository by lazy {
        WalletRepository(
            context,
            secureKeystoreDataSource,
            debugManager,
            inGroupeDatasource,
            analyticsManager,
            coroutineScope,
        )
    }
    val isolationManager: IsolationManager by lazy { IsolationManager(context, robertManager, secureKeystoreDataSource) }

    val cleaDataSource: CleaDataSource = CleaDataSource(
        context,
        EnvConstant.Prod.cleaReportBaseUrl,
        EnvConstant.Prod.cleaStatusBaseUrl,
        analyticsManager,
    )
    val inGroupeDatasource: InGroupeDatasource = InGroupeDatasource(
        context,
        cryptoDataSource,
        EnvConstant.Prod.conversionBaseUrl,
        analyticsManager,
    )
    val serviceDataSource: ServiceDataSource = ServiceDataSource(
        context,
        EnvConstant.Prod.baseUrl,
        analyticsManager,
    )

    val robertManager: RobertManager = RobertManagerImpl(
        context,
        SecureFileEphemeralBluetoothIdentifierDataSource(context, cryptoManager),
        secureKeystoreDataSource,
        SecureFileLocalProximityDataSource(File(context.filesDir, LOCAL_PROXIMITY_DIR), cryptoManager),
        serviceDataSource,
        cleaDataSource,
        cryptoDataSource,
        configurationDataSource,
        calibrationDataSource,
        EnvConstant.Prod.serverPublicKey,
        LocalProximityFilterImpl(),
        analyticsManager,
        coroutineScope = coroutineScope,
    )

    companion object {
        private const val LOCAL_PROXIMITY_DIR = "local_proximity"
    }
}