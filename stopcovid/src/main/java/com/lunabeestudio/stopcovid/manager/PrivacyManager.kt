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
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.coreui.manager.ServerManager
import com.lunabeestudio.stopcovid.model.PrivacySection
import java.lang.reflect.Type
import java.util.Locale

object PrivacyManager : ServerManager() {

    private val _privacySections: MutableLiveData<List<PrivacySection>> = MutableLiveData()
    val privacySections: LiveData<List<PrivacySection>>
        get() = _privacySections

    private var prevLanguage: String? = null

    suspend fun initialize(context: Context) {
        prevLanguage = Locale.getDefault().language
        loadLocal<List<PrivacySection>>(context)?.let {
            setPrivacySection(it)
        }
    }

    private fun setPrivacySection(privacySections: List<PrivacySection>) {
        if (this.privacySections.value != privacySections) {
            _privacySections.postValue(privacySections)
        }
    }

    suspend fun onAppForeground(context: Context) {
        val languageHasChanged = prevLanguage != Locale.getDefault().language
        prevLanguage = Locale.getDefault().language

        if (languageHasChanged) {
            loadLocal<List<PrivacySection>>(context)?.let {
                setPrivacySection(it)
            }
        }

        val hasFetch = fetchLast(context, languageHasChanged)
        if (hasFetch) {
            loadLocal<List<PrivacySection>>(context)?.let {
                setPrivacySection(it)
            }
        }
    }

    override fun folderName(): String = "Privacy"
    override fun prefix(context: Context): String = "privacy-"
    override fun type(): Type = object : TypeToken<List<PrivacySection>>() {}.type
    override fun lastRefreshSharedPrefsKey(): String = Constants.SharedPrefs.LAST_PRIVACY_REFRESH
}