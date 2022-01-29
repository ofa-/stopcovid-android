/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.manager

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.fixFormatter
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLanguage
import okhttp3.OkHttpClient
import java.lang.reflect.Type

typealias LocalizedStrings = Map<String, String>

class StringsManager(okHttpClient: OkHttpClient) : ServerManager<LocalizedStrings>(okHttpClient) {

    var strings: LocalizedStrings = hashMapOf()
        private set(value) {
            if (field != value) {
                _liveStrings.postValue(Event(value))
            }
            field = value
        }

    private val _liveStrings: MutableLiveData<Event<LocalizedStrings>> = MutableLiveData()
    val liveStrings: LiveData<Event<LocalizedStrings>>
        get() = _liveStrings

    private var prevLanguage: String? = null

    suspend fun initialize(context: Context) {
        prevLanguage = context.getApplicationLanguage()
        loadLocal(context)?.let {
            strings = it
        }
    }

    suspend fun getStrings(context: Context): Map<String, String> {
           if (strings.isEmpty())
               initialize(context)
           return strings
    }

    suspend fun onAppForeground(context: Context) {
        val newLanguage = context.getApplicationLanguage()
        val languageHasChanged = prevLanguage != newLanguage

        if (languageHasChanged) {
            loadLocal(context)?.let {
                prevLanguage = newLanguage
                strings = it
            }
        }

        val hasFetch = fetchLast(context, languageHasChanged)
        if (hasFetch) {
            loadLocal(context)?.let {
                prevLanguage = newLanguage
                strings = it
            }
        }
    }

    override fun getUrl(): String = ConfigConstant.Labels.URL
    override val folderName: String = ConfigConstant.Labels.FOLDER
    override val prefix: String = ConfigConstant.Labels.FILE_PREFIX
    override val type: Type = object : TypeToken<LocalizedStrings>() {}.type
    override val lastRefreshSharedPrefsKey: String = UiConstants.SharedPrefs.LAST_STRINGS_REFRESH
    override fun transform(input: String): String = input.fixFormatter()
}
