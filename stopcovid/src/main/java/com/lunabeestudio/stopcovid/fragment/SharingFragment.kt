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

import android.view.Gravity
import android.view.View
import androidx.core.app.ShareCompat
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.mikepenz.fastadapter.GenericItem

class SharingFragment : AboutMainFragment() {

    override fun getTitleKey(): String = "sharingController.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.share
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["sharingController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["sharingController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += buttonItem {
            text = strings["sharingController.buttonAction"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                ShareCompat.IntentBuilder.from(requireActivity())
                    .setType("text/plain")
                    .setText(strings["sharingController.appSharingMessage"])
                    .startChooser()
            }
            identifier = items.size.toLong()
        }

        return items
    }
}