/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.model

sealed class RobertResult {
    open class Success : RobertResult()
    open class Failure(var error: RobertException? = null) : RobertResult()
}

sealed class RobertResultData<T> {
    open class Success<T>(val data: T) : RobertResultData<T>()
    open class Failure<T>(var error: RobertException? = null) : RobertResultData<T>()
}