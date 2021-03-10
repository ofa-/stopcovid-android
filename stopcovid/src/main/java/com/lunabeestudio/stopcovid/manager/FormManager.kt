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
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.model.FormField
import java.lang.reflect.Type

typealias AttestationForm = List<List<FormField>>

object FormManager : RemoteFileManager<AttestationForm>() {

    override val type: Type = object : TypeToken<AttestationForm>() {}.type
    override val localFileName: String = ConfigConstant.Attestations.FILENAME
    override val remoteFileUrl: String = ConfigConstant.Attestations.URL
    override val assetFilePath: String = ConfigConstant.Attestations.ASSET_FILE_PATH

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
}