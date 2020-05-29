/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/20/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.utils

import com.lunabeestudio.domain.extension.safeDestroy
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey

class SelfDestroyCipherInputStream(inputStream: InputStream, cipher: Cipher, private val key: SecretKey)
    : CipherInputStream(inputStream, cipher) {
    override fun close() {
        super.close()
        key.safeDestroy()
    }
}

class SelfDestroyCipherOutputStream(outputStream: OutputStream, cipher: Cipher, private val key: SecretKey)
    : CipherOutputStream(outputStream, cipher) {
    override fun close() {
        super.close()
        key.safeDestroy()
    }
}