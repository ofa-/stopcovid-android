package com.lunabeestudio.stopcovid.manager

import android.content.SharedPreferences
import android.net.UrlQuerySanitizer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lunabeestudio.domain.extension.walletPublicKey
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.model.WalletCertificateUnknownError

object WalletManager {

    private var _walletCertificateLiveData: MutableLiveData<List<WalletCertificate>?> = MutableLiveData(null)
    val walletCertificateLiveData: LiveData<List<WalletCertificate>?>
        get() = _walletCertificateLiveData

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        localKeystoreDataSource: LocalKeystoreDataSource,
    ) {
        localKeystoreDataSource.rawWalletCertificatesLiveData.observe(lifecycleOwner) { rawWalletList ->
            loadFromLocalKeystoreDataSource(rawWalletList)
        }
    }

    private fun loadFromLocalKeystoreDataSource(
        rawWalletList: List<RawWalletCertificate>?
    ) {
        val walletCertificates = rawWalletList?.mapNotNull { rawWallet ->
            val certificate = certificateFromValue(rawWallet.value)
            certificate?.parse()
            certificate
        }
        _walletCertificateLiveData.postValue(walletCertificates)
    }

    fun processCertificateCode(
        sharedPreferences: SharedPreferences,
        robertManager: RobertManager,
        localKeystoreDataSource: LocalKeystoreDataSource,
        certificateCode: String,
    ) {
        val walletCertificate = verifyCertificateCodeValue(sharedPreferences, robertManager.configuration, certificateCode)
        saveCertificate(localKeystoreDataSource, walletCertificate)
    }

    fun extractCertificateCodeFromUrl(urlValue: String): String {
        val sanitizer = UrlQuerySanitizer()
        sanitizer.registerParameter("v") {
            it // Do nothing since there are plenty of non legal characters in this value
        }
        sanitizer.parseUrl(sanitizer.unescape(urlValue))
        return sanitizer.getValue("v") ?: throw WalletCertificateMalformedException()
    }

    fun verifyCertificateCodeValue(
        sharedPreferences: SharedPreferences,
        configuration: Configuration,
        codeValue: String
    ): WalletCertificate {
        val walletCertificate = certificateFromValue(codeValue)
            ?: throw WalletCertificateMalformedException()

        walletCertificate.parse()

        val key = configuration.walletPublicKey(walletCertificate.keyAuthority, walletCertificate.keyCertificateId)
        if (key != null) {
            walletCertificate.verifyKey(key)
        } else {
            throw WalletCertificateUnknownError()
        }

        return walletCertificate
    }

    private fun saveCertificate(localKeystoreDataSource: LocalKeystoreDataSource, walletCertificate: WalletCertificate) {
        val walletCertificates = localKeystoreDataSource.rawWalletCertificates?.toMutableList() ?: mutableListOf()
        walletCertificates.add(RawWalletCertificate(walletCertificate.type, walletCertificate.value, walletCertificate.timestamp))
        localKeystoreDataSource.rawWalletCertificates = walletCertificates
    }

    private fun extractCertificateType(value: String): WalletCertificateType? = WalletCertificateType.values()
        .firstOrNull { it.validationRegexp.matches(value) }

    private fun certificateFromValue(value: String): WalletCertificate? {
        val type: WalletCertificateType = extractCertificateType(value) ?: return null
        return when (type) {
            WalletCertificateType.SANITARY -> SanitaryCertificate(value)
        }
    }

    fun deleteCertificate(localKeystoreDataSource: LocalKeystoreDataSource, walletCertificate: WalletCertificate) {
        val walletCertificates = localKeystoreDataSource.rawWalletCertificates?.toMutableList() ?: mutableListOf()
        walletCertificates.firstOrNull { it.value == walletCertificate.value }?.let { walletCertificates.remove(it) }
        localKeystoreDataSource.rawWalletCertificates = walletCertificates
    }

    fun deleteAllCertificates(localKeystoreDataSource: LocalKeystoreDataSource) {
        localKeystoreDataSource.rawWalletCertificates = emptyList()
    }

}

