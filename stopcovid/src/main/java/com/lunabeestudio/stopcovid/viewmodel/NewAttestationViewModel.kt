/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.stopcovid.manager.FormManager
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

class NewAttestationViewModel(private val secureKeystoreDataSource: SecureKeystoreDataSource) : ViewModel() {

    var shouldSaveInfos: Boolean = secureKeystoreDataSource.saveAttestationData ?: true
    val infos: MutableMap<String, FormEntry> = (secureKeystoreDataSource.savedAttestationData ?: mapOf()).toMutableMap()
    private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT)
    private val timeFormat: DateFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)

    fun generateQrCode() {
        secureKeystoreDataSource.saveAttestationData = shouldSaveInfos
        infos.keys.forEach { key ->
            infos[key] = FormEntry(infos[key]?.value?.trim(), infos[key]!!.type)
        }
        val infosCopy = infos.toMutableMap()
        val now = Date()
        infosCopy["creationDate"] = FormEntry(dateFormat.format(now), "text")
        infosCopy["creationHour"] = FormEntry(timeFormat.format(now), "text")
        secureKeystoreDataSource.attestations = (secureKeystoreDataSource.attestations?.toMutableList() ?: mutableListOf()).apply {
            add(infosCopy)
        }
        if (shouldSaveInfos) {
            infos.remove("datetime")
            secureKeystoreDataSource.savedAttestationData = infos
        }
    }

    fun resetInfos() {
        infos.clear()
        infos.putAll(secureKeystoreDataSource.savedAttestationData ?: mapOf())
    }

    fun areInfosValid(): Boolean {
        return FormManager.form.value?.peekContent()?.all { formFields ->
            formFields.all { formField ->
                !infos[formField.key]?.value.isNullOrBlank()
            }
        } ?: false
    }
}

class NewAttestationViewModelFactory(private val secureKeystoreDataSource: SecureKeystoreDataSource) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NewAttestationViewModel(secureKeystoreDataSource) as T
    }
}