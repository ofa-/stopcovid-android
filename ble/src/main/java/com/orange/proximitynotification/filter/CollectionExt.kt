/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/28 - for the STOP-COVID project
 */

package com.orange.proximitynotification.filter

import kotlin.math.exp
import kotlin.math.ln

internal fun Collection<Number>.softmax(factor: Double): Double {
    if (isEmpty() || factor <= 0.0) {
        return 0.0
    }

    val exponentialSum = fold(0.0) { accumulator, input -> accumulator + exp(input.toDouble() / factor) }
    return factor * ln(exponentialSum / size)
}
