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
import java.util.UUID

object WalletManager {

    private val _walletCertificateLiveData: MutableLiveData<List<WalletCertificate>?> = MutableLiveData(null)
    val walletCertificateLiveData: LiveData<List<WalletCertificate>?>
        get() = _walletCertificateLiveData

    fun initialize(
        lifecycleOwner: LifecycleOwner,
        localKeystoreDataSource: LocalKeystoreDataSource
    ) {
        migrateCertificates(localKeystoreDataSource)
        localKeystoreDataSource.rawWalletCertificatesLiveData.observe(lifecycleOwner) { rawWalletList ->
            lifecycleOwner.lifecycleScope.launch {
                loadFromLocalKeystoreDataSource(rawWalletList)
            }
        }
    }

    // This function adds a way to refresh the liveData if the Keychain wasn't available at app start
    fun refreshWalletIfNeeded(localKeystoreDataSource: LocalKeystoreDataSource) {
        if (localKeystoreDataSource.rawWalletCertificatesLiveData.value != localKeystoreDataSource.rawWalletCertificates
            && !localKeystoreDataSource.rawWalletCertificates.isNullOrEmpty()
        ) {
            localKeystoreDataSource.rawWalletCertificates = localKeystoreDataSource.rawWalletCertificates
        }
    }

    // id + isFavorite migration (null due to reflection)
    private fun migrateCertificates(localKeystoreDataSource: LocalKeystoreDataSource) {
        val certificatesToMigrate = localKeystoreDataSource.rawWalletCertificates?.filter { it.id == null || it.isFavorite == null }
        if (!certificatesToMigrate.isNullOrEmpty()) {
            localKeystoreDataSource.rawWalletCertificates =
                localKeystoreDataSource.rawWalletCertificates?.toMutableList()?.apply {
                    removeAll(certificatesToMigrate)
                    addAll(
                        certificatesToMigrate.map {
                            it.copy(id = it.id ?: UUID.randomUUID().toString(), isFavorite = it.isFavorite ?: false)
                        }
                    )
                }
        }
    }

    private suspend fun loadFromLocalKeystoreDataSource(
        rawWalletList: List<RawWalletCertificate>?
    ) {
        val walletCertificates = rawWalletList?.mapNotNull { rawWallet ->
            try {
                WalletCertificate.createCertificateFromRaw(rawWallet)?.apply {
                    parse()
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
        _walletCertificateLiveData.postValue(walletCertificates)
    }

    fun extractCertificateDataFromUrl(urlValue: String): Pair<String, WalletCertificateType.Format?> {
        val uri = Uri.parse(urlValue)
        var code = uri.fragment

        if (code == null) { // Try the old way
            val sanitizer = UrlQuerySanitizer()
            sanitizer.registerParameter("v") {
                it // Do nothing since there are plenty of non legal characters in this value
            }
            sanitizer.parseUrl(sanitizer.unescape(urlValue))
            code = sanitizer.getValue("v")
        }

        val certificateFormat = uri.lastPathSegment?.let { WalletCertificateType.Format.fromValue(it) }

        return (code ?: throw WalletCertificateMalformedException()) to certificateFormat
    }

    suspend fun verifyAndGetCertificateCodeValue(
        configuration: Configuration,
        codeValue: String,
        dccCertificates: DccCertificates,
        certificateFormat: WalletCertificateType.Format?,
    ): WalletCertificate {
        val walletCertificate = getCertificateFromValue(codeValue)

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
        } else if ((walletCertificate as? EuropeanCertificate)?.greenCertificate?.isFrench == true
            || walletCertificate !is EuropeanCertificate
        ) {
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

    fun toggleFavorite(localKeystoreDataSource: LocalKeystoreDataSource, walletCertificate: EuropeanCertificate) {
        val walletCertificates = localKeystoreDataSource.rawWalletCertificates?.toMutableList() ?: mutableListOf()

        walletCertificates.firstOrNull { it.id == walletCertificate.id }?.let { rawWalletCertificate ->
            if (!walletCertificate.isFavorite) {
                // Remove current favorite if there is one
                walletCertificates.firstOrNull { it.isFavorite }?.let { currentFavorite ->
                    currentFavorite.isFavorite = false
                }
            }

            rawWalletCertificate.isFavorite = !rawWalletCertificate.isFavorite
        }

        localKeystoreDataSource.rawWalletCertificates = walletCertificates
    }

    private suspend fun getCertificateFromValue(value: String): WalletCertificate? {
        return WalletCertificate.createCertificateFromValue(value)
    }

    fun deleteCertificate(localKeystoreDataSource: LocalKeystoreDataSource, walletCertificate: WalletCertificate) {
        val walletCertificates = localKeystoreDataSource.rawWalletCertificates?.toMutableList() ?: mutableListOf()
        walletCertificates.firstOrNull { it.id == walletCertificate.id }?.let { walletCertificates.remove(it) }
        localKeystoreDataSource.rawWalletCertificates = walletCertificates
    }

    fun deleteAllCertificates(localKeystoreDataSource: LocalKeystoreDataSource) {
        localKeystoreDataSource.rawWalletCertificates = emptyList()
    }
}
