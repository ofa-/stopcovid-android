/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/28/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.manager

import android.content.Context
import androidx.annotation.WorkerThread
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.extension.download
import timber.log.Timber

object ConfigManager {

    private const val URL: String = BuildConfig.SERVER_URL + "config.json"

    @WorkerThread
    fun fetchLast(context: Context): String {
        Timber.d("Fetching remote config at $URL")
        return URL.download(context)
    }
}