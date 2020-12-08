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
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.model.Section

object LinksManager : SectionManager() {

    private val _linksSections: MutableLiveData<List<Section>> = MutableLiveData()
    val linksSections: LiveData<List<Section>>
        get() = _linksSections

    override fun setSections(sections: List<Section>) {
        if (this.linksSections.value != sections) {
            _linksSections.postValue(sections)
        }
    }

    override fun folderName(): String = "Links"
    override fun prefix(context: Context): String = "links-"
    override fun urlFolderName(): String = "Links/"
    override fun lastRefreshSharedPrefsKey(): String = Constants.SharedPrefs.LAST_LINKS_REFRESH
}