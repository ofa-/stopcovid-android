/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.extension

import androidx.annotation.NonNull
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.robert.utils.EventObserver

/**
 * Observe an [Event] wrapped in a live data and consume it.
 *
 * @param owner The LifecycleOwner which controls the observer
 * @param skipCurrent If true, do not trigger the observer with the initial live data value
 * @param observer The [Observer] callback
 */
fun <U, T : Event<U?>> LiveData<T>.observeEventAndConsume(
    @NonNull owner: LifecycleOwner,
    skipCurrent: Boolean = true,
    observerId: Int? = null,
    @NonNull observer: Observer<U>,
) {
    val eventObserverId = observerId ?: owner.hashCode()
    if (skipCurrent) {
        this.value?.getContentIfNotHandled(eventObserverId)
    }
    observe(
        owner,
        EventObserver(eventObserverId) {
            observer.onChanged(it)
        }
    )
}
