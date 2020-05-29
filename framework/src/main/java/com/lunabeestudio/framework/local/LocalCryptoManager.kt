/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/20/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.local

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.lunabeestudio.domain.extension.safeUse
import com.lunabeestudio.framework.utils.SelfDestroyCipherInputStream
import com.lunabeestudio.framework.utils.SelfDestroyCipherOutputStream
import com.lunabeestudio.robert.extension.randomize
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.RSAKeyGenParameterSpec
import java.util.Calendar
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal
import javax.security.cert.CertificateException

class LocalCryptoManager(private val appContext: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER).apply {
        this.load(null)
    }

    private val sharedPreferences: SharedPreferences = appContext.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    private val localProtectionKey: SecretKey
        get() = getAesGcmLocalProtectionKey(appContext)

    fun encryptToString(clearText: String): String {
        return encryptToString(clearText.toByteArray())
    }

    fun encryptToString(passphrase: ByteArray, clearPassphrase: Boolean = true): String {
        return Base64.encodeToString(encrypt(passphrase, clearPassphrase), Base64.NO_WRAP)
    }

    fun encryptToFile(clearText: String, targetFile: File) {
        val tmpFile = createTempFile(directory = targetFile.parentFile)
        createCipherOutputStream(tmpFile.outputStream()).use { output ->
            clearText.byteInputStream().use { input ->
                input.copyTo(output)
            }
        }
        tmpFile.renameTo(targetFile)
    }

    @Synchronized
    fun encrypt(passphrase: ByteArray, clearPassphrase: Boolean = true): ByteArray {

        val iv = ByteArray(AES_GCM_IV_LENGTH)
        val cipher = Cipher.getInstance(AES_GCM_CIPHER_TYPE)

        val ciphertext = localProtectionKey.safeUse { secretKey ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                cipher.iv.copyInto(iv)
            } else {
                prng.nextBytes(iv)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            }

            cipher.doFinal(passphrase)
        }

        if (clearPassphrase) {
            passphrase.randomize()
        }

        val encrypted = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, encrypted, 0, iv.size)
        System.arraycopy(ciphertext, 0, encrypted, iv.size, ciphertext.size)

        return encrypted
    }

    fun decrypt(encryptedText: String): ByteArray {
        return decrypt(Base64.decode(encryptedText, Base64.NO_WRAP))
    }

    fun decryptToString(encryptedText: String): String = String(decrypt(encryptedText))

    fun decryptToString(file: File): String {
        val fis = file.inputStream()
        val cis = createCipherInputStream(fis)

        return cis.reader().use {
            it.readText()
        }
    }

    @Synchronized
    fun decrypt(encryptedData: ByteArray): ByteArray {

        val iv: ByteArray = encryptedData.copyOfRange(0,
            AES_GCM_IV_LENGTH)

        val cipher = Cipher.getInstance(AES_GCM_CIPHER_TYPE)
        val ivSpec = GCMParameterSpec(AES_GCM_KEY_SIZE_IN_BITS, iv)

        return localProtectionKey.safeUse { secretKey ->
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            cipher.doFinal(encryptedData,
                AES_GCM_IV_LENGTH, encryptedData.size - AES_GCM_IV_LENGTH)
        }
    }

    /**
     * Returns the AES key used for local storage encryption/decryption with AES/GCM.
     * The key is created if it does not exist already in the keystore.
     * From Marshmallow, this key is generated and operated directly from the android keystore.
     * From KitKat and before Marshmallow, this key is stored in the application shared preferences
     * wrapped by a RSA key generated and operated directly from the android keystore.
     *
     * @param context the context holding the application shared preferences
     */
    @SuppressLint("InlinedApi")
    @Synchronized
    @Throws(KeyStoreException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        IOException::class,
        NoSuchProviderException::class,
        InvalidAlgorithmParameterException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        IllegalBlockSizeException::class,
        UnrecoverableKeyException::class)
    private fun getAesGcmLocalProtectionKey(context: Context): SecretKey {
        val secretKey: SecretKey

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            secretKey = if (keyStore.containsAlias(AES_LOCAL_PROTECTION_KEY_ALIAS)) {
                keyStore.getKey(AES_LOCAL_PROTECTION_KEY_ALIAS, null) as SecretKey
            } else {
                val generator: KeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE_PROVIDER)
                generator.init(
                    KeyGenParameterSpec.Builder(AES_LOCAL_PROTECTION_KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setKeySize(AES_GCM_KEY_SIZE_IN_BITS)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build())
                generator.generateKey()
            }
        } else {
            val wrappedAesKeyString = sharedPreferences.getString(AES_WRAPPED_PROTECTION_KEY_SHARED_PREFERENCE, null)
            if (wrappedAesKeyString != null && keyStore.containsAlias(RSA_WRAP_LOCAL_PROTECTION_KEY_ALIAS)) {
                val privateKey: PrivateKey = keyStore.getKey(RSA_WRAP_LOCAL_PROTECTION_KEY_ALIAS, null) as PrivateKey
                val wrappedAesKey = Base64.decode(wrappedAesKeyString, 0)
                val cipher = Cipher.getInstance(RSA_WRAP_CIPHER_TYPE)
                cipher.init(Cipher.UNWRAP_MODE, privateKey)
                secretKey = cipher.unwrap(wrappedAesKey, "AES", Cipher.SECRET_KEY) as SecretKey
            } else {
                val generator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,
                    ANDROID_KEY_STORE_PROVIDER)
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR_OF_DAY, -26)
                val start: Date = calendar.time
                calendar.add(Calendar.YEAR, 10)
                val end: Date = calendar.time
                @Suppress("DEPRECATION")
                generator.initialize(
                    KeyPairGeneratorSpec.Builder(context)
                        .setAlgorithmParameterSpec(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
                        .setAlias(RSA_WRAP_LOCAL_PROTECTION_KEY_ALIAS)
                        .setSubject(X500Principal("CN=stopcovid-robert-android"))
                        .setStartDate(start)
                        .setEndDate(end)
                        .setSerialNumber(BigInteger.ONE)
                        .build())
                val keyPair: KeyPair = generator.generateKeyPair()
                val aesKeyRaw = ByteArray(AES_GCM_KEY_SIZE_IN_BITS / java.lang.Byte.SIZE)
                prng.nextBytes(aesKeyRaw)
                secretKey = SecretKeySpec(aesKeyRaw, "AES")
                val cipher = Cipher.getInstance(RSA_WRAP_CIPHER_TYPE)
                cipher.init(Cipher.WRAP_MODE, keyPair.public)
                val wrappedAesKey = cipher.wrap(secretKey)
                sharedPreferences.edit()
                    .putString(AES_WRAPPED_PROTECTION_KEY_SHARED_PREFERENCE, Base64.encodeToString(wrappedAesKey, 0))
                    .apply()
            }
        }

        return secretKey
    }

    /**
     * Create a CipherOutputStream instance.
     *
     * @param outputStream the output stream
     */
    @Throws(IOException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        NoSuchPaddingException::class,
        NoSuchProviderException::class,
        KeyStoreException::class,
        IllegalBlockSizeException::class)
    fun createCipherOutputStream(outputStream: OutputStream): OutputStream {
        val cipher = Cipher.getInstance(AES_GCM_CIPHER_TYPE)
        val iv: ByteArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cipher.init(Cipher.ENCRYPT_MODE, localProtectionKey)
            cipher.iv
        } else {
            val iv = ByteArray(AES_GCM_IV_LENGTH)
            prng.nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, localProtectionKey, IvParameterSpec(iv))
            iv
        }

        outputStream.write(iv.size)
        outputStream.write(iv)

        return SelfDestroyCipherOutputStream(outputStream, cipher, localProtectionKey)
    }

    /**
     * Create a CipherInputStream instance.
     *
     * @param inputStream the input stream
     * @return the created InputStream
     */
    @Throws(NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        CertificateException::class,
        InvalidKeyException::class,
        KeyStoreException::class,
        UnrecoverableKeyException::class,
        IllegalBlockSizeException::class,
        NoSuchProviderException::class,
        InvalidAlgorithmParameterException::class,
        IOException::class)
    fun createCipherInputStream(inputStream: InputStream): InputStream {
        inputStream.mark(4 + AES_GCM_IV_LENGTH)
        val ivLen: Int = inputStream.read()
        val iv = ByteArray(ivLen)
        inputStream.read(iv)
        val cipher = Cipher.getInstance(AES_GCM_CIPHER_TYPE)
        val spec: AlgorithmParameterSpec
        spec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            GCMParameterSpec(AES_GCM_KEY_SIZE_IN_BITS, iv)
        } else {
            IvParameterSpec(iv)
        }

        cipher.init(Cipher.DECRYPT_MODE, localProtectionKey, spec)

        return SelfDestroyCipherInputStream(inputStream, cipher, localProtectionKey)
    }

    companion object {
        private const val SHARED_PREF_NAME = "crypto_prefs"

        private const val ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore"
        private const val AES_GCM_CIPHER_TYPE = "AES/GCM/NoPadding"
        private const val AES_GCM_KEY_SIZE_IN_BITS = 128
        private const val AES_GCM_IV_LENGTH = 12
        private const val AES_LOCAL_PROTECTION_KEY_ALIAS = "aes_local_protection"

        private const val RSA_WRAP_LOCAL_PROTECTION_KEY_ALIAS = "rsa_wrap_local_protection"
        private const val RSA_WRAP_CIPHER_TYPE = "RSA/NONE/PKCS1Padding"
        private const val AES_WRAPPED_PROTECTION_KEY_SHARED_PREFERENCE = "aes_wrapped_local_protection"

        private val prng: SecureRandom = SecureRandom()
    }
}