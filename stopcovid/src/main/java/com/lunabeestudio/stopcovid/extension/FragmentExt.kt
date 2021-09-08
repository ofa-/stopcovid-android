/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/31/8 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import androidx.fragment.app.Fragment
import com.lunabeestudio.stopcovid.InjectionContainer

val Fragment.injectionContainer: InjectionContainer
    get() = requireActivity().injectionContainer