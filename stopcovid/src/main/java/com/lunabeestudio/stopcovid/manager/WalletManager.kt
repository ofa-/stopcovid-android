package com.lunabeestudio.stopcovid.manager

import android.net.Uri
import android.net.UrlQuerySanitizer
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.lunabeestudio.domain.extension.walletPublicKey
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.model.DccCertificates
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.model.WalletCertificateNoKeyError
import com.lunabeestudio.stopcovid.model.getForKeyId
import kotlinx.coroutines.launch
import timber.log.Timber

object WalletManager {

    private var _walletCertificateLiveData: MutableLiveData<List<WalletCertificate>?> = MutableLiveData(null)
    val walletCertificateLiveData: LiveData<List<WalletCertificate>?>
        get() = _walletCertificateLiveData

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        localKeystoreDataSource: LocalKeystoreDataSource,
    ) {
        localKeystoreDataSource.rawWalletCertificatesLiveData.observe(lifecycleOwner) { rawWalletList ->
            lifecycleOwner.lifecycleScope.launch {
                loadFromLocalKeystoreDataSource(rawWalletList)
            }
        }
    }

    private suspend fun loadFromLocalKeystoreDataSource(
        rawWalletList: List<RawWalletCertificate>?
    ) {
        val walletCertificates = rawWalletList?.mapNotNull { rawWallet ->
            try {
                val certificate = certificateFromValue(rawWallet.value)
                certificate?.parse()
                certificate
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
        _walletCertificateLiveData.postValue(walletCertificates)
    }

    fun extractCertificateCodeFromUrl(urlValue: String): String {
        var code = Uri.parse(urlValue).fragment

        if (code == null) { // Try the old way
            val sanitizer = UrlQuerySanitizer()
            sanitizer.registerParameter("v") {
                it // Do nothing since there are plenty of non legal characters in this value
            }
            sanitizer.parseUrl(sanitizer.unescape(urlValue))
            code = sanitizer.getValue("v")
        }

        return code ?: throw WalletCertificateMalformedException()
    }

    suspend fun verifyCertificateCodeValue(
        configuration: Configuration,
        codeValue: String,
        dccCertificates: DccCertificates,
        certificateFormat: WalletCertificateType.Format?,
    ): WalletCertificate {
        val walletCertificate = certificateFromValue(codeValue)

        if (walletCertificate == null ||
            (certificateFormat != null && walletCertificate.type.format != certificateFormat)
        ) {
            throw WalletCertificateMalformedException()
        }

        walletCertificate.parse()

        val key: String? = when (walletCertificate) {
            is EuropeanCertificate -> dccCertificates.getForKeyId(walletCertificate.keyCertificateId)
            is FrenchCertificate -> configuration.walletPublicKey(walletCertificate.keyAuthority, walletCertificate.keyCertificateId)
        }

        if (key != null) {
            walletCertificate.verifyKey(key)
        } else if ((walletCertificate as? EuropeanCertificate)?.greenCertificate?.isFrench == true) {
            // Only check French certificates
            throw WalletCertificateNoKeyError()
        }

        return walletCertificate
    }

    fun saveCertificate(localKeystoreDataSource: LocalKeystoreDataSource, walletCertificate: WalletCertificate) {
        val walletCertificates = localKeystoreDataSource.rawWalletCertificates?.toMutableList() ?: mutableListOf()
        walletCertificates.add(walletCertificate.raw)
        localKeystoreDataSource.rawWalletCertificates = walletCertificates
    }

    private suspend fun certificateFromValue(value: String): WalletCertificate? {
        return WalletCertificate.fromValue(value)
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