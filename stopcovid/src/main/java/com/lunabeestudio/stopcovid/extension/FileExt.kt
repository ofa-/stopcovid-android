/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/14/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import java.io.File

fun File.listLogFiles(): List<File>? = listFiles { file ->
    file.extension == "log"
}?.toList()