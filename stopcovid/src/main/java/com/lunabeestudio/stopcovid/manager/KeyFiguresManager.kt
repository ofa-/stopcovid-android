/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.util.MalformedJsonException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import com.lunabeestudio.stopcovid.model.KeyFigure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    val highlightedFigures: KeyFigure?
        get() = _figures.value?.peekContent()?.firstOrNull { it.isFeatured && (it.isHighlighted == true) }

    val featuredFigures: List<KeyFigure>?
        get() = _figures.value?.peekContent()?.filter { it.isFeatured && (it.isHighlighted != true) }?.take(3)

    suspend fun initialize(context: Context) {
        loadLocal(context)?.let { figures ->
            if (_figures.value?.peekContent() != figures) {
                _figures.postValue(Event(figures))
            }
        }
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            loadLocal(context)?.let { figures ->
                if (_figures.value?.peekContent() != figures) {
                    _figures.postValue(Event(figures))
                }
            }
        }
    }

    private suspend fun loadLocal(context: Context): List<KeyFigure>? {
        val keyFiguresFile = File(context.filesDir, cacheFileName)
        return if (keyFiguresFile.exists()) {
            withContext(Dispatchers.IO) {
                try {
                    Timber.v("Loading $keyFiguresFile to object")
                    gson.fromJson<List<KeyFigure>>(File(context.filesDir, cacheFileName).readText(), typeKeyFigure)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
        } else {
            Timber.v("Nothing to load")
            null
        }
    }

    private suspend fun fetchLast(context: Context): Boolean {
        val filename = "$cacheFileName.bck"
        val tmpFile = File(context.filesDir, filename)
        return try {
            Timber.v("Fetching remote data at $url")
            url.saveTo(context, tmpFile)
            if (fileNotCorrupted(context, filename)) {
                tmpFile.copyTo(File(context.filesDir, cacheFileName), overwrite = true, bufferSize = 4 * 1024)
            } else {
                throw MalformedJsonException("Failed to parse key figure JSON")
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Fetching failed")
            false
        } finally {
            tmpFile.delete()
        }
    }

    private suspend fun fileNotCorrupted(context: Context, filename: String): Boolean {
        return withContext(Dispatchers.IO) {
            val content = gson.fromJson<List<KeyFigure?>>(File(context.filesDir, filename).readText(), typeKeyFigure)
            content.none {
                it == null
            }
        }
    }
}
