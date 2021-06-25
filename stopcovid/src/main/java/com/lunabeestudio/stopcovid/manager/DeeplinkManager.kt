/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/1/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.net.Uri

object DeeplinkManager {
    const val DEEPLINK_CODE_PARAMETER: String = "code"
    const val DEEPLINK_CERTIFICATE_FORMAT_PARAMETER: String = "certificateFormat"
    const val DEEPLINK_CERTIFICATE_ORIGIN_PARAMETER: String = "origin"

    fun transformFragmentToCodeParam(uri: Uri): Uri {
        return uri.buildUpon()
            .fragment(null)
            .appendQueryParameter(DEEPLINK_CODE_PARAMETER, uri.encodedFragment)
            .appendQueryParameter(DEEPLINK_CERTIFICATE_FORMAT_PARAMETER, uri.lastPathSegment?.removeSuffix(".html"))
            .build()
    }

    enum class Origin {
        EXTERNAL, UNIVERSAL
    }
}