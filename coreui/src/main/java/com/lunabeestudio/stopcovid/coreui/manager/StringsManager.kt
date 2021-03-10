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
import com.lunabeestudio.stopcovid.coreui.extension.getFirstSupportedLanguage
import java.lang.reflect.Type

object StringsManager : ServerManager() {

    var strings: HashMap<String, String> = hashMapOf()
        private set(value) {
            if (field != value) {
                _liveStrings.postValue(Event(value))
            }
            field = value
        }

    private val _liveStrings: MutableLiveData<Event<HashMap<String, String>>> = MutableLiveData()
    val liveStrings: LiveData<Event<HashMap<String, String>>>
        get() = _liveStrings

    private var prevLanguage: String? = null

    suspend fun initialize(context: Context) {
        prevLanguage = context.getFirstSupportedLanguage()
        loadLocal<HashMap<String, String>>(context)?.let {
            strings = it
        }
    }

    suspend fun getStrings(context: Context): HashMap<String, String> {
           if (strings.isEmpty())
               initialize(context)
           return strings
    }

    suspend fun onAppForeground(context: Context) {
        val newLanguage = context.getFirstSupportedLanguage()
        val languageHasChanged = prevLanguage != newLanguage

        if (languageHasChanged) {
            loadLocal<HashMap<String, String>>(context)?.let {
                strings = it
            }
        }

        val hasFetch = fetchLast(context, languageHasChanged)
        if (hasFetch) {
            loadLocal<HashMap<String, String>>(context)?.let {
                prevLanguage = newLanguage
                strings = it
            }
        }
    }

    override val url: String = ConfigConstant.Labels.URL
    override val folderName: String = ConfigConstant.Labels.FOLDER
    override val prefix: String = ConfigConstant.Labels.FILE_PREFIX
    override val type: Type = object : TypeToken<HashMap<String, String>>() {}.type
    override val lastRefreshSharedPrefsKey: String = UiConstants.SharePrefs.LAST_STRINGS_REFRESH
    override fun transform(input: String): String = input.fixFormatter()
}
