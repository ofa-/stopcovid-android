/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/11 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

fun Map<String, String>.stringsFormat(key: String, vararg args: Any?): String? {
    return this[key].formatOrNull(*args)
}