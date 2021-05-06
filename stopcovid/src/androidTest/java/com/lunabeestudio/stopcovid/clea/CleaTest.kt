/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/02/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.clea

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeS
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.framework.crypto.BouncyCastleCryptoDataSource
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.local.datasource.SecureFileEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.framework.manager.LocalProximityFilterImpl
import com.lunabeestudio.framework.remote.datasource.CleaDataSource
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.stopcovid.coreui.currentEnv
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.manager.CalibDataSource
import com.lunabeestudio.stopcovid.manager.ConfigDataSource
import com.lunabeestudio.stopcovid.manager.VenuesManager
import com.lunabeestudio.support.framework.local.datasource.SupportSecureFileLocalProximityDataSource
import com.lunabeestudio.support.robert.SupportRobertManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.io.File

class CleaTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val cryptoManager: LocalCryptoManager by lazy {
        LocalCryptoManager(context)
    }

    private val secureKeystoreDataSource: SecureKeystoreDataSource by lazy {
        SecureKeystoreDataSource(context, cryptoManager)
    }

    private val supportRobertManager: SupportRobertManager by lazy {
        SupportRobertManager(
            context as RobertApplication,
            SecureFileEphemeralBluetoothIdentifierDataSource(context, cryptoManager),
            secureKeystoreDataSource,
            SupportSecureFileLocalProximityDataSource(File(context.filesDir, "local_proximity_test"), cryptoManager),
            ServiceDataSource(
                context,
                sharedPrefs.currentEnv.baseUrl,
                sharedPrefs.currentEnv.certificateSha256
            ),
            CleaDataSource(
                context,
                sharedPrefs.currentEnv.cleaReportBaseUrl,
                sharedPrefs.currentEnv.cleaReportCertificateSha256,
                "https://lunabee.studio/wazzup/7jhtAhj098oss177/clea/shouldbeok/",
                sharedPrefs.currentEnv.cleaStatusCertificateSha256
            ),
            BouncyCastleCryptoDataSource(),
            ConfigDataSource,
            CalibDataSource,
            sharedPrefs.currentEnv.serverPublicKey,
            LocalProximityFilterImpl()
        )
    }

    @Before
    fun setUp() {
        supportRobertManager.eraseRemoteAlert()
        VenuesManager.clearAllData(sharedPrefs, secureKeystoreDataSource)
    }

    @After
    fun tearDown() {
        supportRobertManager.eraseRemoteAlert()
    }

    @Test
    fun clea_matching_venues() {
        addVenue(
            "AKIK6oBFzttNk2qufUM59D51sL9HuLJ2OXbS1Jbi68RutMcXO1jW399Vw7486Sktdljx7YXhA6l9kPN7wRaouYv1Qf//zy25pbnCD+ICSU8TwupeYTPG5LMXb4jCjBZkyEml8eUOPONtZ7aumJA=",
            System.currentTimeMillis().unixTimeMsToNtpTimeS()
        )
        addVenue(
            "AGUKYcvMWTLqqUAEMbqcq0saGh3vPKTcfPSTCfsQcodWkmUie/y47eeZ3rcgj3Sl9xcnagy9mSQkCGCHltHbLBbAn1m95o4MYxYndNCdGz0FzODBl/0CE/OKtPDrS+wI5wt7cZEbQqd1bhJ8HH4dYKdL0Ibh2ipNzzIIVRTOU3LZwX3Yla5+k7AdP26fHQJGUh1BJz3UEJgePesl4/WIGZg/d2tCdO2SBH2yYVSkEw==",
            System.currentTimeMillis().unixTimeMsToNtpTimeS()
        )

        val result = runBlocking {
            supportRobertManager.getCurrentCleaStatusRisk(context as RobertApplication)
        }
        assert(result is RobertResultData.Success)

        val riskLevel = (result as RobertResultData.Success).data
        assert(supportRobertManager.cleaStatusIteration != null)
    }

    private fun addVenue(base64: String, timeNTP: Long) {
        try {
            VenuesManager.processVenue(
                robertManager = context.robertManager(),
                secureKeystoreDataSource = secureKeystoreDataSource,
                base64,
                0,
                timeNTP.ntpTimeSToUnixTimeS()
            )
        } catch (e: Exception) {
            Timber.e(e, "Error with URL ${base64} !!")
        }
    }

}