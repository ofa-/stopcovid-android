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
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.stopcovid.coreui.extension.getFirstSupportedLanguage
import com.lunabeestudio.stopcovid.coreui.manager.ServerManager
import com.lunabeestudio.stopcovid.model.Section
import java.lang.reflect.Type

abstract class SectionManager : ServerManager() {

    private var prevLanguage: String? = null

    suspend fun initialize(context: Context) {
        prevLanguage = context.getFirstSupportedLanguage()
        loadLocal<List<Section>>(context)?.let {
            setSections(it)
        }
    }

    abstract fun setSections(sections: List<Section>)

    suspend fun onAppForeground(context: Context) {
        val newLanguage = context.getFirstSupportedLanguage()
        val languageHasChanged = prevLanguage != newLanguage

        if (languageHasChanged) {
            loadLocal<List<Section>>(context)?.let {
                setSections(it)
            }
        }

        val hasFetch = fetchLast(context, languageHasChanged)
        if (hasFetch) {
            loadLocal<List<Section>>(context)?.let {
                prevLanguage = newLanguage
                setSections(it)
            }
        }
    }

    override val type: Type = object : TypeToken<List<Section>>() {}.type
}