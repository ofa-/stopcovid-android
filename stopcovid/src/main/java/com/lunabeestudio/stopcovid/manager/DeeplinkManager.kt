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
    fun transformAnchorParam(uri: Uri): Uri = Uri.parse(transformAnchorParam(uri.toString()))
    fun transformAnchorParam(uri: String): String = if (uri.contains('?')) {
        uri.replace("#", "&code=")
    } else {
        uri.replace("#", "?code=")
    }
}