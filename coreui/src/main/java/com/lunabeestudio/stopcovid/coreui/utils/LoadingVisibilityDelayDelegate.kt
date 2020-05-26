/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/18/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Helper class to show/hide loading state or view with a delay to avoid blink on tiny loading time
 *
 * @property minLoadingShowDurationMs Minimum time in ms that the loading will be displayed
 * @property delayBeforeShowMs Delay before showing the loading in ms
 * @property showLoading Block called to display the loading
 * @property hideLoading Block called to hide the loading
 */
class LoadingVisibilityDelayDelegate(
    var showLoading: () -> Unit,
    var hideLoading: () -> Unit,
    var minLoadingShowDurationMs: Long = DEFAULT_MIN_LOADING_SHOW_DURATION_MS,
    var delayBeforeShowMs: Long = DEFAULT_DELAY_BEFORE_SHOW_MS) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val job: Job = Job()

    private var pendingShow: AtomicBoolean = AtomicBoolean(false)

    private var showJob: Job? = null
    private var hideJob: Job? = null

    /**
     * Request the loading to be hide. Cancels the loading show request if [delayBeforeShowMs] has
     * not been reached and immediately call [hideLoading] or wait until [showJob] is done to call
     * the [hideLoading] block.
     */
    fun delayHideLoading() {
        if (pendingShow.get()) {
            showJob?.cancel("Hide called")
            hideLoading()
        } else {
            hideJob = launch {
                showJob?.join()
                hideLoading()
            }
        }
    }

    /**
     * Request the loading to be show. Cancels the pending hide request if exist and call
     * [showLoading] block after [delayBeforeShowMs] has been reached.
     */
    fun delayShowLoading() {
        if (pendingShow.compareAndSet(false, true)) {
            hideJob?.cancel("Show called")

            showJob = launch {
                delay(delayBeforeShowMs)
                showLoading()
                pendingShow.set(false)
                delay(minLoadingShowDurationMs)
            }

            showJob?.invokeOnCompletion {
                pendingShow.set(false)
            }
        }
    }

    companion object {
        const val DEFAULT_MIN_LOADING_SHOW_DURATION_MS: Long = 500L
        const val DEFAULT_DELAY_BEFORE_SHOW_MS: Long = 500L
    }
}