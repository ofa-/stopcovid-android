/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/2/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.model

/**
 * Generic result wrapper to wrap data fetched asynchronously with a status.
 *
 * @param T Type of the wrapped data. Might be [Unit] if the expected result does not contain any data.
 */
sealed class TacResult<out T> {

    /**
     * Loading implementation of [TacResult]
     *
     * @param T @inheritDoc
     * @property partialData The data already fetched or null if non applicable
     * @property progress The current progress
     */
    data class Loading<out T>(val partialData: T? = null, val progress: Float? = null) : TacResult<T>()

    /**
     * Success implementation of [TacResult]
     *
     * @param T @inheritDoc
     * @property successData The final non-null data
     */
    data class Success<out T>(val successData: T) : TacResult<T>()

    /**
     * Failure implementation of [TacResult]
     *
     * @param T @inheritDoc
     * @property throwable The throwable that caused the failure or null if non applicable
     * @property failureData The final data or null if non applicable
     */
    data class Failure<out T>(val throwable: Throwable? = null, val failureData: T? = null) : TacResult<T>() {
        /**
         * Secondary constructor to instantiate a failure result from an error string
         */
        @Suppress("unused")
        constructor(message: String) : this(throwable = Exception(message))
    }

    /**
     * Common getter for the data of any result states.
     */
    val data: T?
        get() {
            return when (this) {
                is Loading -> partialData
                is Success -> successData
                is Failure -> failureData
            }
        }

    fun <U> mapData(transform: (T?) -> U?): TacResult<U> {
        return when (this) {
            is Failure -> Failure(throwable, transform(failureData))
            is Loading -> Loading(transform(partialData), progress)
            is Success -> {
                val transformed = transform(successData)
                if (transformed == null) {
                    Failure(null, transformed)
                } else {
                    Success(transformed)
                }
            }
        }
    }
}