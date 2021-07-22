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
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLanguage
import com.lunabeestudio.stopcovid.coreui.manager.ServerManager
import com.lunabeestudio.stopcovid.model.Section
import java.lang.reflect.Type

typealias Sections = List<Section>

abstract class SectionManager : ServerManager<Sections>() {

    private var prevLanguage: String? = null

    suspend fun initialize(context: Context) {
        prevLanguage = context.getApplicationLanguage()
        loadLocal(context)?.let {
            setSections(it)
        }
    }

    abstract fun setSections(sections: Sections)

    suspend fun onAppForeground(context: Context) {
        val newLanguage = context.getApplicationLanguage()
        val languageHasChanged = prevLanguage != newLanguage

        if (languageHasChanged) {
            loadLocal(context)?.let {
                setSections(it)
            }
        }

        val hasFetch = fetchLast(context, languageHasChanged)
        if (hasFetch) {
            loadLocal(context)?.let {
                prevLanguage = newLanguage
                setSections(it)
            }
        }
    }

    override val type: Type = object : TypeToken<Sections>() {}.type
}