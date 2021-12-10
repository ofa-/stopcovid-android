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
import androidx.preference.PreferenceManager
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.toKeyFigures
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFiguresNotAvailableException
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.stopcovid.widgetshomescreen.KeyFiguresWidget
import keynumbers.Keynumbers
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.GZIPInputStream

class KeyFiguresManager(serverManager: ServerManager) :
    RemoteProtoGzipManager<Keynumbers.KeyNumbersMessage, List<KeyFigure>>(serverManager) {

    override fun getLocalFileName(context: Context): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = preferences.chosenPostalCode?.let { KeyFigure.getDepartmentKeyFromPostalCode(it) }
        val suffix = key ?: ConfigConstant.KeyFigures.NATIONAL_SUFFIX
        return ConfigConstant.KeyFigures.LOCAL_FILENAME_TEMPLATE.format(suffix)
    }

    override fun getRemoteFileUrl(context: Context): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val key = preferences.chosenPostalCode?.let { KeyFigure.getDepartmentKeyFromPostalCode(it) }
        val codePath = if (key != null) "/$key" else ""
        val suffix = key ?: ConfigConstant.KeyFigures.NATIONAL_SUFFIX
        return ConfigConstant.KeyFigures.URL.format(codePath, suffix)
    }

    override fun getAssetFilePath(context: Context): String? = null

    private val _figures: MutableLiveData<Event<TacResult<List<KeyFigure>>>> = MutableLiveData()
    val figures: LiveData<Event<TacResult<List<KeyFigure>>>>
        get() = _figures

    val highlightedFigures: KeyFigure?
        get() = _figures.value?.peekContent()?.data?.firstOrNull { it.isFeatured && (it.isHighlighted == true) }

    val featuredFigures: List<KeyFigure>?
        get() = _figures.value?.peekContent()?.data?.filter { it.isFeatured && (it.isHighlighted != true) }?.take(3)

    suspend fun initialize(context: Context) {
        val figuresResult = loadLocalResult(context)
        if (_figures.value?.peekContent() != figuresResult) {
            _figures.postValue(Event(figuresResult))
        }
        KeyFiguresWidget.updateWidget(context)
    }

    suspend fun onAppForeground(context: Context) {
        fetchLast(context)
        initialize(context) // call initialize even on error to load local data if available (on postal code change for example)
    }

    suspend fun loadLocalResult(context: Context): TacResult<List<KeyFigure>> {
        val localFile = File(context.filesDir, getLocalFileName(context))
        val keyFigures = try {
            getMappedProtoMessage(localFile)
        } catch (e: IOException) {
            Timber.w("${localFile.path} not found, falling back to national key figures")
            null
        }

        if (keyFigures == null) {
            // Try to fallback to national key figures
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val key = preferences.chosenPostalCode?.let { KeyFigure.getDepartmentKeyFromPostalCode(it) }
            val suffix = key ?: ConfigConstant.KeyFigures.NATIONAL_SUFFIX

            if (suffix != ConfigConstant.KeyFigures.NATIONAL_SUFFIX) {
                val fallbackFilePath = ConfigConstant.KeyFigures.LOCAL_FILENAME_TEMPLATE.format(ConfigConstant.KeyFigures.NATIONAL_SUFFIX)
                val fallbackFile = File(context.filesDir, fallbackFilePath)

                val natKeyFigures = try {
                    getMappedProtoMessage(fallbackFile)
                } catch (e: FileNotFoundException) {
                    Timber.e(e)
                    null
                }
                return TacResult.Failure(throwable = KeyFiguresNotAvailableException(), failureData = natKeyFigures)
            }

            return TacResult.Failure(throwable = KeyFiguresNotAvailableException(), failureData = null)
        } else {
            return TacResult.Success(successData = keyFigures)
        }
    }

    override fun parseProtoGzipStream(gzipInputStream: GZIPInputStream): Keynumbers.KeyNumbersMessage {
        return Keynumbers.KeyNumbersMessage.parseFrom(gzipInputStream)
    }

    override fun Keynumbers.KeyNumbersMessage.mapProtoToApp(): List<KeyFigure> {
        return this.toKeyFigures()
    }
}
