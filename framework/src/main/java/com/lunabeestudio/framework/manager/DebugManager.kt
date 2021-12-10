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
import com.lunabeestudio.domain.extension.safeIsDestroyed
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.TacResult
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
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.SecretKey

class DebugManager(
    appContext: Context,
    private val keystoreDataSource: SecureKeystoreDataSource,
    val logsDir: File,
    private val cryptoManager: LocalCryptoManager,
) {

    private val cryptoPrefs = appContext.getSharedPreferences(LocalCryptoManager.SHARED_PREF_NAME, Context.MODE_PRIVATE)
    private val robertPrefs = appContext.getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE)
    private val debugPrefs = appContext.getSharedPreferences(DEBUG_PREF_NAME, Context.MODE_PRIVATE)
    private val appPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val keyStore = KeyStore.getInstance(LocalCryptoManager.ANDROID_KEY_STORE_PROVIDER).apply {
        this.load(null)
    }
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private val masterKeyExist: Boolean
        get() {
            val hasAesAlias = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && keyStore.containsAlias("aes_local_protection")
            val hasRsaAlias = Build.VERSION.SDK_INT < Build.VERSION_CODES.M &&
                keyStore.containsAlias("rsa_wrap_local_protection") &&
                cryptoPrefs.getString("aes_wrapped_local_protection", null) != null
            return hasAesAlias || hasRsaAlias
        }

    private var debugFile: File = File(logsDir, appPrefs.currentLogFileName).also { file ->
        file.createNewFile()
        val newSessionBlock = StringBuilder().apply {
            appendLine("++++ Start session  ${dateTimeFormat.format(Date())} ++++")
            appendCommonData(file)
        }.toString()
        file.appendTextAndRotate(newSessionBlock)
    }

    private fun StringBuilder.appendCommonData(file: File) {
        appendLine("${Build.MODEL} - API ${Build.VERSION.SDK_INT}")
        appendLine("keystore aliases = ${keyStore.aliases().toList().joinToString()}")
        keyStore.aliases().toList().forEach { alias ->
            try {
                val key = keyStore.getKey(alias, null) as? SecretKey
                appendLine("$alias = ${key?.javaClass?.simpleName}, ${key?.algorithm}, ${key?.format}, ${key?.safeIsDestroyed}")
            } catch (e: Exception) {
                appendLine("$alias = ${e.javaClass.simpleName} ${e.message}")
            }

            try {
                if (masterKeyExist) {
                    val clearText = debugPrefs.getString(SHARED_PREFS_CRYPTO_DEBUG, null)?.let { cryptoManager.decryptToString(it) }
                    val isTestOk = clearText?.equals(CRYPTO_DEBUG_TEST_STRING)
                    appendLine("Crypto test = $isTestOk")
                    if (isTestOk == false) {
                        resetCryptoTest()
                    }
                }
            } catch (e: Exception) {
                appendLine("Crypto test = ${e.javaClass.simpleName} ${e.message}")
                resetCryptoTest()
            }
        }
        appendLine("Available space = ${StatFs(file.path).availableBytes / 1024 / 1024}mB")
        appendCertificateNoDecrypt()
        appendLine()
    }

    init {
        if (debugPrefs.getString(SHARED_PREFS_CRYPTO_DEBUG, null) == null && masterKeyExist) {
            resetCryptoTest()
        }
    }

    private fun resetCryptoTest() {
        debugPrefs.edit {
            putString(SHARED_PREFS_CRYPTO_DEBUG, cryptoManager.encryptToString(CRYPTO_DEBUG_TEST_STRING))
        }
    }

    private fun StringBuilder.log() {
        if (!debugFile.exists()) {
            debugFile.apply {
                createNewFile()
                val stringBuilder = StringBuilder()
                stringBuilder.appendLine("++++ Restart session  ${dateTimeFormat.format(Date())} ++++")
                stringBuilder.appendCommonData(this)
                debugFile.appendTextAndRotate(stringBuilder.toString())
            }
        }

        insert(0, "[EVENT]\nDate = ${dateTimeFormat.format(Date())}\n")
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

        appendCertificateNoDecrypt()
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
                    "certificates nodecrypt = ${keystoreDataSource.db.certificateRoomDao().getAll().joinToString { it.uid }}"
                )
            }
        }
    }

    fun logSaveCertificates(rawWalletCertificate: RawWalletCertificate, info: String? = null) {
        StringBuilder().apply {
            appendLine("• Save certificate ${rawWalletCertificate.id} (${rawWalletCertificate.type})")
            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    fun logDeleteCertificates(rawWalletCertificate: RawWalletCertificate, info: String? = null) {
        StringBuilder().apply {
            appendLine("• Delete certificate ${rawWalletCertificate.id} (${rawWalletCertificate.type})")
            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    fun logCertificateMigrated(rawWalletCertificate: List<RawWalletCertificate>, info: String? = null) {
        StringBuilder().apply {
            appendLine("• Migrate certificate ${rawWalletCertificate.joinToString { "${it.id} (${it.type})" }}")
            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    fun logObserveCertificate(rawWalletCertificateResult: TacResult<List<RawWalletCertificate>>, info: String? = null) {
        StringBuilder().apply {
            appendLine("• Observe certificate")
            appendCertificatesResult(rawWalletCertificateResult)

            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    fun logOpenWalletContainer(rawWalletCertificateResult: TacResult<List<RawWalletCertificate>>, info: String? = null) {
        StringBuilder().apply {
            appendLine("• Open container")
            appendCertificatesResult(rawWalletCertificateResult)
            if (info != null) {
                appendLine("info = $info")
            }
            log()
        }
    }

    private fun StringBuilder.appendCertificatesResult(rawWalletCertificateResult: TacResult<List<RawWalletCertificate>>) {
        appendLine("result = ${rawWalletCertificateResult.print()}")
        if (rawWalletCertificateResult is TacResult.Failure) {
            val error = rawWalletCertificateResult.throwable
            appendLine("error = ${error?.javaClass?.simpleName} ${error?.message}")
        }
    }

    fun logReinitializeWallet() {
        StringBuilder().apply {
            appendLine("• Reinitialize container")
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

    private var SharedPreferences.currentLogFileName: String
        get() = getString(CURRENT_LOG_FILENAME, null) ?: LOGS_FILENAME.first()
        set(value) {
            edit { putString(CURRENT_LOG_FILENAME, value) }
            debugFile = File(logsDir, appPrefs.currentLogFileName)
        }

    private fun TacResult<List<RawWalletCertificate>>.print(): String =
        """${this.javaClass.simpleName} : ${data?.joinToString { "${it.id} (${it.type})" }}"""

    companion object {
        private const val CURRENT_LOG_FILENAME: String = "currentLogFilename"

        private const val DEBUG_PREF_NAME: String = "9b47e1c9-6740-4bc0-ae61-059d80f78a1b"
        private const val SHARED_PREFS_CRYPTO_DEBUG: String = "6ddbaaaf-0669-457b-afa9-79e826352911"
        private const val CRYPTO_DEBUG_TEST_STRING: String = "945cb3bc-853b-44e9-8cd4-a4002f803d70"

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
