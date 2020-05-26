/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.extension.fillItems
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.manager.PrivacyManager
import com.lunabeestudio.stopcovid.model.PrivacySection
import com.mikepenz.fastadapter.GenericItem

class PrivacyFragment : MainFragment() {

    override fun getTitleKey(): String = "onboarding.privacyController.title"

    private var privacySections: List<PrivacySection> = PrivacyManager.getPrivacySections()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PrivacyManager.privacySections.observe(viewLifecycleOwner, Observer { privacySections ->
            this.privacySections = privacySections
            refreshScreen()
        })
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
        }
        privacySections.fillItems(items)

        return items
    }
}