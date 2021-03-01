/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/18/02 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid

import android.util.Log
import timber.log.Timber

class WarnTree : Timber.DebugTree() {

    override fun isLoggable(priority: Int): Boolean {
        return priority >= Log.WARN
    }
}