/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import com.lunabeestudio.stopcovid.model.KeyFigure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type

object KeyFiguresManager {

    private var gson: Gson = Gson()

    private const val cacheFileName: String = "key-figures.json"
    private const val url: String = com.lunabeestudio.stopcovid.coreui.BuildConfig.BASE_URL + BuildConfig.KEY_FIGURES_PATH
    private val typeKeyFigure: Type = object : TypeToken<List<KeyFigure>>() {}.type

    private val _figures: MutableLiveData<Event<List<KeyFigure>>> = MutableLiveData()
    val figures: LiveData<Event<List<KeyFigure>>>
        get() = _figures

    fun init(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            loadLocal(context)
        }
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun appForeground(context: Context) {
        if (fetchLast(context)) {
            loadLocal(context)?.let { figures ->
                if (_figures.value?.peekContent() != figures) {
                    _figures.postValue(Event(figures))
                }
            }
        }
    }

    private fun loadLocal(context: Context): List<KeyFigure>? {
        return if (File(context.filesDir, cacheFileName).exists()) {
            try {
                Timber.d("Loading file to object")
                gson.fromJson<List<KeyFigure>>(File(context.filesDir, cacheFileName).readText(), typeKeyFigure)
            } catch (e: java.lang.Exception) {
                Timber.e(e)
                null
            }
        } else {
            Timber.d("Nothing to load")
            null
        }
    }

    @WorkerThread
    private fun fetchLast(context: Context): Boolean {
        return try {
            val filename: String = cacheFileName
            Timber.d("Fetching remote data at $url")
            url.saveTo(context, File(context.filesDir, filename))
            true
        } catch (e: Exception) {
            Timber.e(e, "Fetching failed")
            false
        }
    }

}
