/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.utils

/**
 *  As default Lazy use SynchronizedLazyImpl (which correspond to LazyThreadSafetyMode.SYNCHRONIZED mode),
 *  so if you use it in this way you are going to be thread safe.
 *
 *  In case you are using this in the Main Thread and you are sure that it’s not going to be used
 *  in different threads, then you can avoid all of this overhead to make it Thread Safe and just
 *  use this [lazyFast] function
 *
 *  @see LazyThreadSafetyMode
 *  @see Lazy
 */
fun <T> lazyFast(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)