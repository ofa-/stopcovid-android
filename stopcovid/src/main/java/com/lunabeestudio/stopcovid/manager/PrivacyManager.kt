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

class PrivacyManager(okHttpClient: OkHttpClient) : SectionManager(okHttpClient) {

    private val _privacySections: MutableLiveData<Sections> = MutableLiveData()
    val privacySections: LiveData<Sections>
        get() = _privacySections

    override fun setSections(sections: Sections) {
        if (this.privacySections.value != sections) {
            _privacySections.postValue(sections)
        }
    }

    override fun getUrl(): String = ConfigConstant.Privacy.URL
    override val folderName: String = ConfigConstant.Privacy.FOLDER
    override val prefix: String = ConfigConstant.Privacy.FILE_PREFIX
    override val lastRefreshSharedPrefsKey: String = Constants.SharedPrefs.LAST_PRIVACY_REFRESH
}