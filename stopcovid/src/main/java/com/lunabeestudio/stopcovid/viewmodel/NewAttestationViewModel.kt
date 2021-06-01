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
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.manager.AttestationsManager
import com.lunabeestudio.stopcovid.manager.FormManager
import com.lunabeestudio.stopcovid.model.FormField
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

class NewAttestationViewModel(private val secureKeystoreDataSource: SecureKeystoreDataSource) : ViewModel() {

    var shouldSaveInfos: Boolean = secureKeystoreDataSource.saveAttestationData ?: false
    val infos: MutableMap<String, FormEntry> = (secureKeystoreDataSource.savedAttestationData ?: mapOf()).toMutableMap()
    private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT)
    private val timeFormat: DateFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)

    fun getInfoForFormField(formField: FormField): FormEntry? {
        return infos[formField.dataKeyValue]?.takeIf { it.key == formField.key }
    }

    fun generateQrCode(robertManager: RobertManager, strings: LocalizedStrings) {
        secureKeystoreDataSource.saveAttestationData = shouldSaveInfos
        infos.keys.forEach { key ->
            infos[key] = FormEntry(infos[key]?.value?.trim(), infos[key]!!.type, key)
        }
        val infosCopy = infos.toMutableMap()
        val now = Calendar.getInstance().time
        timeFormat.apply {
            timeZone = TimeZone.getDefault()
        }
        infosCopy[Constants.Attestation.KEY_CREATION_DATE] = FormEntry(
            dateFormat.format(now),
            "text",
            Constants.Attestation.KEY_CREATION_DATE
        )
        infosCopy[Constants.Attestation.KEY_CREATION_HOUR] = FormEntry(
            timeFormat.format(now),
            "text",
            Constants.Attestation.KEY_CREATION_HOUR
        )
        AttestationsManager.addAttestation(robertManager, secureKeystoreDataSource, strings, infosCopy)
        if (shouldSaveInfos) {
            infos.remove(Constants.Attestation.KEY_DATE_TIME)
            infos.remove(Constants.Attestation.DATA_KEY_REASON)
            secureKeystoreDataSource.savedAttestationData = infos
        }
    }

    fun pickFormEntry(key: String, formEntry: FormEntry) {
        infos[key] = formEntry
    }

    fun resetInfos() {
        infos.clear()
        infos.putAll(secureKeystoreDataSource.savedAttestationData ?: mapOf())
    }

    fun areInfosValid(): Boolean {
        return FormManager.form.value?.peekContent()?.all { formFields ->
            formFields.all { formField ->
                !infos[formField.dataKeyValue]?.value.isNullOrBlank()
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