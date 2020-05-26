/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.domain.extension

import javax.crypto.SecretKey
import javax.security.auth.DestroyFailedException

fun SecretKey.safeDestroy() {
    try {
        destroy()
    } catch (e: DestroyFailedException) {
        // Destroy not implemented
    } catch (e: NoSuchMethodError) {
        // Destroy not implemented
    }
}

fun <T> SecretKey.safeUse(block: (SecretKey) -> T): T {
    try {
        return block(this)
    } finally {
        try {
            destroy()
        } catch (e: DestroyFailedException) {
            // Destroy not implemented
        } catch (e: NoSuchMethodError) {
            // Destroy not implemented
        }
    }
}