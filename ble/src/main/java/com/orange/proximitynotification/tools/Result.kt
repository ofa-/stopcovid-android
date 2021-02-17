/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2021/01/20 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.tools

internal sealed class Result<out T> {

    fun valueOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    data class Success<out T>(val value: T) : Result<T>()
    data class Failure(val throwable: Throwable?) : Result<Nothing>()
}