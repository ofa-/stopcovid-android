/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/26/8 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.manager

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.StatFs
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.extension.safeIsDestroyed
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Type
import java.security.KeyStore
import java.text.DateFormat
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.SecretKey

class DebugManager(
    appContext: Context,
    private val keystoreDataSource: SecureKeystoreDataSource,
    val logsDir: File,
    private val cryptoManager: LocalCryptoManager,
) {

    private val gson = Gson()
    private val cryptoPrefs = appContext.getSharedPreferences(LocalCryptoManager.SHARED_PREF_NAME, Context.MODE_PRIVATE)
    private val robertPrefs = appContext.getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE)
    private val appPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val keyStore = KeyStore.getInstance(LocalCryptoManager.ANDROID_KEY_STORE_PROVIDER).apply {
        this.load(null)
    }

    private var debugFile: File = File(logsDir, appPrefs.currentLogFileName).also { file ->
        file.createNewFile()
        val newSessionBlock = StringBuilder().apply {
            appendLine("++++ Start session  ${DateFormat.getDateTimeInstance().format(Date())} ++++")
            appendLine("${Build.MODEL} - API ${Build.VERSION.SDK_INT}")
            appendLine("keystore aliases = ${keyStore.aliases().toList().joinToString()}")
            keyStore.aliases().toList().forEach { alias ->
                try {
                    val key = keyStore.getKey(alias, null) as? SecretKey
                    appendLine("$alias = ${key?.javaClass?.simpleName}, ${key?.algorithm}, ${key?.format}, ${key?.safeIsDestroyed}")
                } catch (e: Exception) {
                    appendLine("$alias = ${e.javaClass.simpleName}")
                }
            }
            appendLine("Available space = ${StatFs(file.path).availableBytes / 1024 / 1024}mB")
            appendCertificateNoDecrypt()
            appendLine()
        }.toString()
        file.appendTextAndRotate(newSessionBlock)
    }

    private suspend fun StringBuilder.log() {
        if (!debugFile.exists()) {
            debugFile.apply {
                createNewFile()
                appendLine("++++ Restart session  ${DateFormat.getDateTimeInstance().format(Date())} ++++")
                appendLine("${Build.MODEL} - API ${Build.VERSION.SDK_INT}")
                appendLine("")
            }
        }

        insert(0, "[EVENT]\nDate = ${DateFormat.getDateTimeInstance().format(Date())}\n")
        appendLine("[DATA]")
        appendLine("keystore aliases = ${keyStore.aliases().toList().joinToString()}")
        keyStore.aliases().toList().forEach { alias ->
            try {
                val key = keyStore.getKey(alias, null) as? SecretKey
                appendLine("$alias = ${key?.javaClass?.simpleName}, ${key?.algorithm}, ${key?.format}, ${key?.safeIsDestroyed}")
            } catch (e: Exception) {
                appendLine("$alias = ${e.javaClass.simpleName}")
            }
        }

        try {
            appendLine("certificates = ${keystoreDataSource.rawWalletCertificates().joinToString { "${it.id} (${it.type})" }}")
        } catch (e: Exception) {
            appendLine("couldn't decrypt certificates")
        }

        appendCertificateNoDecrypt()
        appendOldCertificateNoCache()
        appendLine("app pref = ${appPrefs.all}")
        appendLine("crypto pref = ${cryptoPrefs.all}")
        val isRegistered = try {
            keystoreDataSource.isRegistered
        } catch (e: Exception) {
            null
        }
        appendLine("is registered = $isRegistered")
        appendLine("Available space = ${StatFs(debugFile.path).availableBytes / 1024 / 1024}mB")
        appendLine()
        debugFile.appendTextAndRotate(this.toString())
    }

    @Suppress("DEPRECATION")
    private fun StringBuilder.appendOldCertificateNoCache() {
        val certificatesLength = robertPrefs.getString(
            SecureKeystoreDataSource.SHARED_PREF_KEY_WALLET_CERTIFICATES,
            ""
        )?.length
        try {
            val certificateData = getEncryptedValue<List<RawWalletCertificate>>(
                key = SecureKeystoreDataSource.SHARED_PREF_KEY_WALLET_CERTIFICATES,
                type = object : TypeToken<List<RawWalletCertificate>>() {}.type,
            )?.joinToString { "${it.id} (${it.type})" }
            appendLine(
                "old certificates nocache ($certificatesLength) = $certificateData"
            )
        } catch (e: Exception) {
            appendLine(
                "old certificates nocache ($certificatesLength) [FAILURE] = " +
                    "${e.javaClass.simpleName}, " +
                    "${e.message}"
            )
        }
    }

    @Suppress("DEPRECATION")
    fun oldCertificateInSharedPrefs(): Boolean =
        (
            robertPrefs.getString(
                SecureKeystoreDataSource.SHARED_PREF_KEY_WALLET_CERTIFICATES,
                ""
            )?.length ?: 0
            ) > 0

    @Suppress("DEPRECATION")
    fun oldVenuesInSharedPrefs(): Boolean =
        (
            robertPrefs.getString(
                SecureKeystoreDataSource.SHARED_PREF_KEY_VENUES_QR_CODE,
                ""
            )?.length ?: 0
            ) > 0

    @Suppress("DEPRECATION")
    fun oldAttestationsInSharedPrefs(): Boolean =
        (
            robertPrefs.getString(
                SecureKeystoreDataSource.SHARED_PREF_KEY_ATTESTATIONS_V2,
                ""
            )?.length ?: 0
            ) > 0

    private fun StringBuilder.appendCertificateNoDecrypt() {
        runBlocking {
            withContext(Dispatchers.IO) {
                appendLine(
                    "certificates nodecrypt = ${
                    keystoreDataSource.db.certificateRoomDao().getAll().joinToString { it.uid }
                    }"
                )
            }
        }
    }

    suspend fun logSaveCertificates(rawWalletCertificate: RawWalletCertificate, info: String? = null) {
        StringBuilder().apply {
            appendLine("• Save certificate ${rawWalletCertificate.id} (${rawWalletCertificate.type})")
            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    suspend fun logDeleteCertificates(rawWalletCertificate: RawWalletCertificate, info: String? = null) {
        StringBuilder().apply {
            appendLine("• Delete certificate ${rawWalletCertificate.id} (${rawWalletCertificate.type})")
            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    suspend fun logCertificateMigrated(rawWalletCertificate: List<RawWalletCertificate>, info: String? = null) {
        StringBuilder().apply {
            appendLine("• Migrate certificate ${rawWalletCertificate.joinToString { "${it.id} (${it.type})" }}")
            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    suspend fun logObserveCertificate(rawWalletCertificate: List<RawWalletCertificate>?, info: String? = null) {
        StringBuilder().apply {
            appendLine("• Observe certificate ${rawWalletCertificate?.joinToString { it.id }}")
            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    suspend fun logOpenWalletContainer(info: String? = null) {
        StringBuilder().apply {
            appendLine("• Open container")
            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    private fun File.appendTextAndRotate(string: String) {
        appendText(string)
        val fileSizeInMb = length().toDouble() / 1024 / 1024
        if (fileSizeInMb > 2) {
            appPrefs.currentLogFileName = when (appPrefs.currentLogFileName) {
                LOGS_FILENAME.first() -> LOGS_FILENAME.last()
                else -> LOGS_FILENAME.first()
            }
        }
    }

    private fun <T> getEncryptedValue(key: String, type: Type): T? {
        val encryptedText = robertPrefs.getString(key, null)
        return if (encryptedText != null) {
            runBlocking {
                if (type == ByteArray::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    cryptoManager.decrypt(encryptedText) as? T
                } else {
                    val decryptedString = cryptoManager.decryptToString(encryptedText)
                    gson.fromJson<T>(decryptedString, type)
                }
            }
        } else {
            null
        }
    }

    private var SharedPreferences.currentLogFileName: String
        get() = getString(CURRENT_LOG_FILENAME, null) ?: LOGS_FILENAME.first()
        set(value) {
            edit { putString(CURRENT_LOG_FILENAME, value) }
            debugFile = File(logsDir, appPrefs.currentLogFileName)
        }

    companion object {
        private const val CURRENT_LOG_FILENAME: String = "currentLogFilename"
        private val LOGS_FILENAME: List<String> = listOf("event_logs_0.log", "event_logs_1.log")

        suspend fun zip(files: List<File>, toZipFile: File) {
            @Suppress("BlockingMethodInNonBlockingContext")
            withContext(Dispatchers.IO) {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(toZipFile.path))).use { out ->
                    for (file in files) {
                        FileInputStream(file).use { fi ->
                            BufferedInputStream(fi).use { origin ->
                                val entry = ZipEntry(file.name)
                                out.putNextEntry(entry)
                                origin.copyTo(out, 1024)
                            }
                        }
                    }
                }
            }
        }
    }
}