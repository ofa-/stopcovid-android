/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/30/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.stopcovid.extension.isOld
import com.lunabeestudio.stopcovid.extension.isRecent
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.support.extension.doNotCheckCertificatesKey
import com.lunabeestudio.support.robert.SupportRobertManager
import org.junit.Assert
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class WalletManagerTest {

    private fun Context.robertManager(): SupportRobertManager = (applicationContext as RobertApplication).robertManager as SupportRobertManager

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context).apply {
        doNotCheckCertificatesKey = true
    }

    @Test
    fun sanitary_url_parsing() {
        val configuration = context.robertManager().configuration
        assertThrows<WalletCertificateMalformedException> {
            WalletManager.verifyCertificateCodeValue(sharedPreferences, configuration, "")
        }
        assertThrows<WalletCertificateMalformedException> {
            WalletManager.verifyCertificateCodeValue(sharedPreferences,
                configuration,
                WalletManager.extractCertificateCodeFromUrl("https://bonjour.tousanticovid.gouv.fr/app/wallet?v="))
        }
        assertThrows<WalletCertificateMalformedException> {
            WalletManager.verifyCertificateCodeValue(
                sharedPreferences,
                configuration,
                WalletManager.extractCertificateCodeFromUrl("https://bonjour.tousanticovid.gouv.fr/app/wallet?q=DC04DHI0TST11E3C1E3CB201FRF0OLIVIER%3CGS%3EF1ESTFORT%3CGS%3EF225111980F3MF494309%3CGS%3EF5NF6220320210852%3CUS%3EZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A")
            )
        }
        assertThrows<WalletCertificateMalformedException> {
            WalletManager.verifyCertificateCodeValue(
                sharedPreferences,
                configuration,
                WalletManager.extractCertificateCodeFromUrl("https://bonjour.tousanticovid.gouv.fr/app/wallet?v=DC04DHIS0TST11E3C1E3CB201FRF0OLIVIER%3CGS%3EF1ESTFORT%3CGS%3EF225111980F3MF494309%3CGS%3EF5NF6220320210852%3CUS%3EZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A")
            )
        }
        assertThrows<WalletCertificateMalformedException> {
            WalletManager.verifyCertificateCodeValue(
                sharedPreferences,
                configuration,
                WalletManager.extractCertificateCodeFromUrl("https://bonjour.tousanticovid.gouv.fr/app/wallet?v=DC04DHI0TST11E3C1E3CB301FRF0Q2%3CGS%3EF1ESTENCOREPLUSFORT%3CGS%3EF225111980F3MF494309%3CGS%3EF5NF6300320211452%3CUS%3EZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A")
            )
        }

        val dateFormat = SimpleDateFormat("ddMMyyyyHHmm", Locale.US)
        val oldDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8L)
        var walletCertificate = WalletManager.verifyCertificateCodeValue(
            sharedPreferences,
            configuration,
            WalletManager.extractCertificateCodeFromUrl("https://bonjour.tousanticovid.gouv.fr/app/wallet?v=DC04DHI0TST11E3C1E3CB201FRF0JEAN%20LOUIS/EDOUARD%1DF1DUPOND%1DF225111980F3MF494309%1DF5NF6${
                dateFormat.format(
                    oldDate
                )
            }%1FZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A")
        )
        assert(walletCertificate is SanitaryCertificate)
        assert(walletCertificate.keyAuthority == "DHI0")
        assert(walletCertificate.keyCertificateId == "TST1")
        assert(walletCertificate.keySignature == "ZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A")
        assert(walletCertificate.timestamp == dateFormat.parse(dateFormat.format(oldDate))!!.time)
        assert(walletCertificate.type == WalletCertificateType.SANITARY)
        assert(
            walletCertificate.value == "DC04DHI0TST11E3C1E3CB201FRF0JEAN LOUIS/EDOUARD\u001DF1DUPOND\u001DF225111980F3MF494309\u001DF5NF6${
                dateFormat.format(
                    oldDate
                )
            }\u001FZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A"
        )
        assert((walletCertificate as SanitaryCertificate).firstName == "JEAN LOUIS, EDOUARD")
        assert(walletCertificate.name == "DUPOND")
        assert(walletCertificate.birthDate?.time == 343954800000L)
        assert(walletCertificate.gender == "M")
        assert(walletCertificate.testResult == "N")
        assert(walletCertificate.analysisDate == dateFormat.parse(dateFormat.format(oldDate))!!.time)
        assert(walletCertificate.analysisCode == "94309")
        assert(!walletCertificate.isRecent(context.robertManager().configuration))
        assert(walletCertificate.isOld(context.robertManager().configuration))

        val recentDate = System.currentTimeMillis()
        walletCertificate = WalletManager.verifyCertificateCodeValue(
            sharedPreferences,
            configuration,
            WalletManager.extractCertificateCodeFromUrl("https://bonjour.tousanticovid.gouv.fr/app/wallet?v=DC04DHI0TST11E3C1E3CB201FRF0JEAN%20LOUIS/EDOUARD%1DF1DUPOND%1DF225111980F3FF494309%1DF5NF6${
                dateFormat.format(
                    recentDate
                )
            }%1FZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A")
        )
        assert(walletCertificate is SanitaryCertificate)
        assert(walletCertificate.keyAuthority == "DHI0")
        assert(walletCertificate.keyCertificateId == "TST1")
        assert(walletCertificate.keySignature == "ZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A")
        assert(walletCertificate.timestamp == dateFormat.parse(dateFormat.format(recentDate))!!.time)
        assert(walletCertificate.type == WalletCertificateType.SANITARY)
        assert(
            walletCertificate.value == "DC04DHI0TST11E3C1E3CB201FRF0JEAN LOUIS/EDOUARD\u001DF1DUPOND\u001DF225111980F3FF494309\u001DF5NF6${
                dateFormat.format(
                    recentDate
                )
            }\u001FZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A"
        )
        assert((walletCertificate as SanitaryCertificate).firstName == "JEAN LOUIS, EDOUARD")
        assert(walletCertificate.name == "DUPOND")
        assert(walletCertificate.birthDate?.time == 343954800000L)
        assert(walletCertificate.gender == "F")
        assert(walletCertificate.testResult == "N")
        assert(walletCertificate.analysisDate == dateFormat.parse(dateFormat.format(recentDate))!!.time)
        assert(walletCertificate.analysisCode == "94309")
        assert(walletCertificate.isRecent(context.robertManager().configuration))
        assert(!walletCertificate.isOld(context.robertManager().configuration))
    }

    private inline fun <reified T : Exception> assertThrows(runnable: () -> Any?) {
        try {
            runnable.invoke()
        } catch (e: Throwable) {
            if (e is T) {
                return
            }
            Assert.fail(
                "expected ${T::class.qualifiedName} but caught " +
                    "${e::class.qualifiedName} instead"
            )
        }
        Assert.fail("expected ${T::class.qualifiedName}")
    }
}