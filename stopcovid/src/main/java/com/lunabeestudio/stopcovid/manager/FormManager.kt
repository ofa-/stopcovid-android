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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import com.lunabeestudio.stopcovid.model.FormField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type

typealias AttestationForm = List<List<FormField>>

object FormManager {

    private var gson: Gson = Gson()

    private const val cacheFileName: String = "form.json"
    private const val assetFolderName: String = "Form"
    private const val url: String = com.lunabeestudio.stopcovid.coreui.BuildConfig.SERVER_URL + BuildConfig.FORM_PATH
    private val typeKeyFigure: Type = object : TypeToken<AttestationForm>() {}.type

    private val _form: MutableLiveData<Event<AttestationForm>> = MutableLiveData()
    val form: LiveData<Event<AttestationForm>>
        get() = _form

    suspend fun initialize(context: Context) {
        loadLocal(context)?.let { form ->
            if (_form.value?.peekContent() != form) {
                _form.postValue(Event(form))
            }
        }
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            loadLocal(context)?.let { form ->
                if (_form.value?.peekContent() != form) {
                    _form.postValue(Event(form))
                }
            }
        }
    }

    private suspend fun loadLocal(context: Context): AttestationForm? {
        val formFile = File(context.filesDir, cacheFileName)
        return if (formFile.exists()) {
            withContext(Dispatchers.IO) {
                try {
                    Timber.v("Loading $formFile to object")
                    gson.fromJson<AttestationForm>(formFile.readText(), typeKeyFigure)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
        } else {
            getDefaultAssetFile(context)
        }
    }

    private suspend fun getDefaultAssetFile(context: Context): AttestationForm? {
        return withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            gson.fromJson<AttestationForm>(context.assets.open("$assetFolderName/$cacheFileName").use {
                it.readBytes().toString(Charsets.UTF_8)
            }, typeKeyFigure)
        }
    }

    private suspend fun fetchLast(context: Context): Boolean {
        return try {
            val filename: String = cacheFileName
            Timber.v("Fetching remote data at $url")
            url.saveTo(context, File(context.filesDir, filename))
            true
        } catch (e: Exception) {
            Timber.e(e, "Fetching failed")
            false
        }
    }

    fun clearLocal(context: Context) {
        File(context.filesDir, cacheFileName).delete()
    }
}