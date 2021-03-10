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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
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

    override val url: String = ConfigConstant.KeyFigures.URL
    override val folderName: String = ConfigConstant.KeyFigures.FOLDER
    override val prefix: String = ConfigConstant.KeyFigures.FILE_PREFIX
    override val lastRefreshSharedPrefsKey: String = Constants.SharedPrefs.LAST_MORE_KEY_FIGURES_REFRESH
}