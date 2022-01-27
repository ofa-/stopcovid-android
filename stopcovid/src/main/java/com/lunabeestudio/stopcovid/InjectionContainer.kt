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

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.framework.crypto.BouncyCastleCryptoDataSource
import com.lunabeestudio.framework.local.BlacklistDatabase
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.local.datasource.SecureFileEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.framework.local.datasource.SecureFileLocalProximityDataSource
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.framework.manager.DebugManager
import com.lunabeestudio.framework.manager.LocalProximityFilterImpl
import com.lunabeestudio.framework.remote.datasource.CleaDataSource
import com.lunabeestudio.framework.remote.datasource.DccLightDataSource
import com.lunabeestudio.framework.remote.datasource.DummyDccLightDataSource
import com.lunabeestudio.framework.remote.datasource.InGroupeDatasource
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.robert.datasource.RemoteDccLightDataSource
import com.lunabeestudio.robert.datasource.RobertCalibrationDataSource
import com.lunabeestudio.robert.datasource.RobertConfigurationDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
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
import com.lunabeestudio.stopcovid.usecase.CleanAndRenewActivityPassUseCase
import com.lunabeestudio.stopcovid.usecase.GenerateActivityPassUseCase
import com.lunabeestudio.stopcovid.usecase.GenerateMultipassUseCase
import com.lunabeestudio.stopcovid.usecase.GetCloseMultipassProfilesUseCase
import com.lunabeestudio.stopcovid.usecase.GetMultipassProfilesUseCase
import com.lunabeestudio.stopcovid.usecase.GetFilteredMultipassProfileFromIdUseCase
import com.lunabeestudio.stopcovid.usecase.GetSmartWalletCertificateUseCase
import com.lunabeestudio.stopcovid.usecase.SmartWalletNotificationUseCase
import com.lunabeestudio.stopcovid.usecase.VerifyAndGetCertificateCodeValueUseCase
import com.lunabeestudio.stopcovid.usecase.VerifyCertificateUseCase
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class InjectionContainer(private val context: StopCovid, val coroutineScope: CoroutineScope) {
    private val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

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
    val calibrationManager: CalibrationManager by lazy { CalibrationManager(serverManager.okHttpClient) }
    val configManager: ConfigManager by lazy { ConfigManager(serverManager.okHttpClient) }
    val analyticsManager: AnalyticsManager by lazy { AnalyticsManager(serverManager.okHttpClient, context) }
    val cryptoManager: LocalCryptoManager by lazy { LocalCryptoManager(context) }
    val certificatesDocumentsManager: CertificatesDocumentsManager by lazy { CertificatesDocumentsManager(serverManager) }
    val dccCertificatesManager: DccCertificatesManager by lazy { DccCertificatesManager(serverManager) }
    val calibrationDataSource: RobertCalibrationDataSource by lazy { TacCalibrationDataSource(calibrationManager) }
    val configurationDataSource: RobertConfigurationDataSource by lazy { TacConfigurationDataSource(configManager) }
    val sharedCryptoDataSource: SharedCryptoDataSource = BouncyCastleCryptoDataSource()
    val secureKeystoreDataSource: SecureKeystoreDataSource by lazy { SecureKeystoreDataSource(context, cryptoManager, ConcurrentHashMap()) }
    val logsDir: File by lazy { File(context.filesDir, Constants.Logs.DIR_NAME) }
    val debugManager: DebugManager by lazy { DebugManager(context, secureKeystoreDataSource, logsDir, cryptoManager) }
    val attestationRepository: AttestationRepository by lazy { AttestationRepository(secureKeystoreDataSource, context) }
    val venueRepository: VenueRepository by lazy { VenueRepository(secureKeystoreDataSource, sharedPrefs) }
    val walletRepository: WalletRepository by lazy {
        WalletRepository(
            context = context,
            localKeystoreDataSource = secureKeystoreDataSource,
            debugManager = debugManager,
            remoteConversionDataSource = inGroupeDatasource,
            analyticsManager = analyticsManager,
            coroutineScope = coroutineScope,
            remoteDccLightDataSource = remoteDccLightDataSource,
            robertManager = robertManager,
        )
    }
    private val blacklistDatabase = BlacklistDatabase.build(context)
    val blacklistDCCManager: BlacklistDCCManager by lazy {
        BlacklistDCCManager(
            context = context,
            serverManager = serverManager,
            dao = blacklistDatabase.europeanCertificateBlacklistRoomDao(),
            sharedPreferences = sharedPrefs,
        )
    }
    val blacklist2DDOCManager: Blacklist2DDOCManager by lazy {
        Blacklist2DDOCManager(
            context = context,
            serverManager = serverManager,
            dao = blacklistDatabase.frenchCertificateBlacklistRoomDao(),
            sharedPreferences = sharedPrefs,
        )
    }

    private val remoteDccLightDataSource: RemoteDccLightDataSource by lazy {
        if (EnvConstant.Prod.activityPassBaseUrl.isNotBlank()) {
            DccLightDataSource(sharedCryptoDataSource, EnvConstant.Prod.activityPassBaseUrl, serverManager.okHttpClient, analyticsManager)
        } else {
            DummyDccLightDataSource()
        }
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
        sharedCryptoDataSource,
        EnvConstant.Prod.conversionBaseUrl,
        analyticsManager,
    )
    val serviceDataSource: ServiceDataSource = ServiceDataSource(
        context,
        EnvConstant.Prod.baseUrl,
        analyticsManager,
    )

    val robertManager: RobertManager = RobertManagerImpl(
        application = context,
        localEphemeralBluetoothIdentifierDataSource = SecureFileEphemeralBluetoothIdentifierDataSource(context, cryptoManager),
        localKeystoreDataSource = secureKeystoreDataSource,
        localLocalProximityDataSource = SecureFileLocalProximityDataSource(File(context.filesDir, LOCAL_PROXIMITY_DIR), cryptoManager),
        serviceDataSource = serviceDataSource,
        cleaDataSource = cleaDataSource,
        sharedCryptoDataSource = sharedCryptoDataSource,
        configurationDataSource = configurationDataSource,
        calibrationDataSource = calibrationDataSource,
        serverPublicKey = EnvConstant.Prod.serverPublicKey,
        localProximityFilter = LocalProximityFilterImpl(),
        analyticsManager = analyticsManager,
        coroutineScope = coroutineScope,
        venueRepository = venueRepository,
    )

    val cleanAndRenewActivityPassUseCase: CleanAndRenewActivityPassUseCase
        get() = CleanAndRenewActivityPassUseCase(
            walletRepository = walletRepository,
            blacklistDCCManager = blacklistDCCManager,
            dccCertificatesManager = dccCertificatesManager,
            robertManager = robertManager,
            generateActivityPassUseCase = generateActivityPassUseCase,
        )

    val verifyCertificateUseCase: VerifyCertificateUseCase
        get() = VerifyCertificateUseCase(
            dccCertificatesManager = dccCertificatesManager,
            robertManager = robertManager,
        )

    val generateActivityPassUseCase: GenerateActivityPassUseCase
        get() = GenerateActivityPassUseCase(
            walletRepository = walletRepository,
            verifyCertificateUseCase = verifyCertificateUseCase,
        )

    val verifyAndGetCertificateCodeValueUseCase: VerifyAndGetCertificateCodeValueUseCase
        get() = VerifyAndGetCertificateCodeValueUseCase(
            verifyCertificateUseCase = verifyCertificateUseCase,
        )

    val getSmartWalletCertificateUseCase: GetSmartWalletCertificateUseCase
        get() = GetSmartWalletCertificateUseCase(
            walletRepository = walletRepository,
            blacklistDCCManager = blacklistDCCManager,
            robertManager = robertManager,
        )

    val smartWalletNotificationUseCase: SmartWalletNotificationUseCase
        get() = SmartWalletNotificationUseCase(
            robertManager = robertManager,
            sharedPreferences = sharedPrefs,
            getSmartWalletCertificateUseCase = getSmartWalletCertificateUseCase,
        )

    val getFilteredMultipassProfileFromIdUseCase: GetFilteredMultipassProfileFromIdUseCase
        get() = GetFilteredMultipassProfileFromIdUseCase(
            robertManager = robertManager,
            walletRepository = walletRepository,
            blacklistDCCManager = blacklistDCCManager,
        )

    val getMultipassProfilesUseCase: GetMultipassProfilesUseCase
        get() = GetMultipassProfilesUseCase(
            walletRepository = walletRepository,
        )

    val generateMultipassUseCase: GenerateMultipassUseCase
        get() = GenerateMultipassUseCase(
            walletRepository = walletRepository,
            verifyCertificateUseCase = verifyCertificateUseCase,
        )

    val getCloseMultipassProfilesUseCase: GetCloseMultipassProfilesUseCase
        get() = GetCloseMultipassProfilesUseCase()

    companion object {
        private const val LOCAL_PROXIMITY_DIR = "local_proximity"
    }
}
