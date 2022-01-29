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
import okhttp3.OkHttpClient

class MoreKeyFiguresManager(okHttpClient: OkHttpClient) : SectionManager(okHttpClient) {

    private val _moreKeyFiguresSections: MutableLiveData<Sections> = MutableLiveData()
    val moreKeyFiguresSections: LiveData<Sections>
        get() = _moreKeyFiguresSections

    override fun setSections(sections: Sections) {
        if (this.moreKeyFiguresSections.value != sections) {
            _moreKeyFiguresSections.postValue(sections)
        }
    }

    override fun getUrl(): String {
        return ConfigConstant.MoreKeyFigures.URL
    }

    override val folderName: String = ConfigConstant.MoreKeyFigures.FOLDER
    override val prefix: String = ConfigConstant.MoreKeyFigures.FILE_PREFIX
    override val lastRefreshSharedPrefsKey: String = Constants.SharedPrefs.LAST_MORE_KEY_FIGURES_REFRESH
}
