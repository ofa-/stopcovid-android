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
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.model.Section

object MoreKeyFiguresManager : SectionManager() {

    private val _moreKeyFiguresSections: MutableLiveData<List<Section>> = MutableLiveData()
    val moreKeyFiguresSections: LiveData<List<Section>>
        get() = _moreKeyFiguresSections

    override fun setSections(sections: List<Section>) {
        if (this.moreKeyFiguresSections.value != sections) {
            _moreKeyFiguresSections.postValue(sections)
        }
    }

    override fun folderName(): String = "MoreKeyFigures"
    override fun prefix(context: Context): String = "morekeyfigures-"
    override fun urlFolderName(): String = BuildConfig.MORE_KEY_FIGURES_FOLDER
    override fun lastRefreshSharedPrefsKey(): String = Constants.SharedPrefs.LAST_MORE_KEY_FIGURES_REFRESH
}