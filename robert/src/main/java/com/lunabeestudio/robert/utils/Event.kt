/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/08/10 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.utils

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class Event<out T>(private val content: T) {

    private val hasBeenHandled: MutableSet<Int> = mutableSetOf()

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(id: Int): T? {
        return if (hasBeenHandled.contains(id)) {
            null
        } else {
            hasBeenHandled.add(id)
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}