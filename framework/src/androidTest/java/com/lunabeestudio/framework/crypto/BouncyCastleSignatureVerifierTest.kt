/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/29/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BouncyCastleSignatureVerifierTest {
    @Test
    fun verify_signature() {
        val result = BouncyCastleSignatureVerifier.verifySignature(
            PUB_KEY,
            MESSAGE,
            SIGNATURE,
            PUB_KEY_ALGORITHM,
            SIGNATURE_ALGORITHM,
        )

        assertThat(result).isTrue()
    }

    companion object {
        const val PUB_KEY: String = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE6lPsrLPPis5Wl9u8Y7THfNIIfeEyq3q72MpzUm3gvr6ctYk4d3OQhn76GKSjSYX/0ZRC/cYO9479K0SUzYZ9yg=="
        const val PUB_KEY_ALGORITHM: String = "ECDSA"
        const val MESSAGE: String = "DC04DHI0TST11E3C1E3CB201FRF0JEAN LOUIS/EDOUARD\u001DF1DUPOND\u001DF225111980F3MF494309\u001DF5NF6110320211452"
        const val SIGNATURE: String = "ZCQ5EDEXRCRYMU4U5U4YQSF5GOE2PMFFC6PDWOMZK64434TUCJWQLIXCRYMA5TWVT7TEZSF2S3ZCJSYK3JYFOBVUHNOEXQMEKWQDG3A"
        const val SIGNATURE_ALGORITHM: String = "SHA256withECDSA"
    }
}
