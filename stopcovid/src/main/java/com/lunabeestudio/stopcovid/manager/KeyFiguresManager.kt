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
import com.lunabeestudio.stopcovid.model.KeyFigure
import java.lang.reflect.Type

object KeyFiguresManager : RemoteFileManager<List<KeyFigure>>() {

    override val type: Type = object : TypeToken<List<KeyFigure>>() {}.type
    override val localFileName: String = ConfigConstant.KeyFigures.MASTER_LOCAL_FILENAME
    override val remoteFileUrl: String = ConfigConstant.KeyFigures.MASTER_URL
    override val assetFilePath: String? = null

    private val _figures: MutableLiveData<Event<List<KeyFigure>>> = MutableLiveData()
    val figures: LiveData<Event<List<KeyFigure>>>
        get() = _figures

    val highlightedFigures: KeyFigure?
        get() = _figures.value?.peekContent()?.firstOrNull { it.isFeatured && (it.isHighlighted == true) }

    val featuredFigures: List<KeyFigure>?
        get() = _figures.value?.peekContent()?.filter { it.isFeatured && (it.isHighlighted != true) }?.take(3)

    suspend fun initialize(context: Context) {
        loadLocal(context)?.let { figures ->
            if (_figures.value?.peekContent() != figures) {
                _figures.postValue(Event(figures))
            }
        }
    }

    suspend fun onAppForeground(context: Context) {
        if (fetchLast(context)) {
            loadLocal(context)?.let { figures ->
                if (_figures.value?.peekContent() != figures) {
                    _figures.postValue(Event(figures))
                }
            }
        }
    }
}
