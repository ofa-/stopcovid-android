/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.manager

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.stopcovid.coreui.R
import com.lunabeestudio.stopcovid.coreui.UiConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.util.Locale

class StringsManager : ServerManager() {

    companion object {
        private var _strings: HashMap<String, String> = hashMapOf()
            set(value) {
                strings.postValue(value)
                field = value
            }
        val strings: MutableLiveData<HashMap<String, String>> = MutableLiveData()

        private var prevLanguage: String? = null

        fun getStrings(): HashMap<String, String> = _strings

        fun init(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                prevLanguage = Locale.getDefault().language
                _strings = StringsManager().loadLocal(context)
            }
        }

        fun appForeground(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                if (StringsManager().fetchLast(context, Locale.getDefault().language) || prevLanguage != Locale.getDefault().language) {
                    prevLanguage = Locale.getDefault().language
                    _strings = StringsManager().loadLocal(context)
                }
            }
        }
    }

    override fun folderName(): String = "Strings"
    override fun prefix(context: Context): String = context.getString(R.string.string_prefix)
    override fun fallbackFileName(context: Context): String = "${prefix(context)}${UiConstants.DEFAULT_LANGUAGE}${extension()}"
    override fun type(): Type = object : TypeToken<HashMap<String, String>>() {}.type
    override fun lastRefreshSharedPrefsKey(): String = UiConstants.SharePrefs.LAST_STRINGS_REFRESH

    override fun transform(input: String): String = input.fixFormatter()
}

private fun String.fixFormatter() = this.replace("%@", "%s")