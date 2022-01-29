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

class LinksManager(okHttpClient: OkHttpClient) : SectionManager(okHttpClient) {

    private val _linksSections: MutableLiveData<Sections> = MutableLiveData()
    val linksSections: LiveData<Sections>
        get() = _linksSections

    override fun setSections(sections: Sections) {
        if (this.linksSections.value != sections) {
            _linksSections.postValue(sections)
        }
    }

    override fun getUrl(): String = ConfigConstant.Links.URL

    override val folderName: String = ConfigConstant.Links.FOLDER
    override val prefix: String = ConfigConstant.Links.FILE_PREFIX
    override val lastRefreshSharedPrefsKey: String = Constants.SharedPrefs.LAST_LINKS_REFRESH
}
