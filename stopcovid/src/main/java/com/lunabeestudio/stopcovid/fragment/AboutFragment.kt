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
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class AboutFragment : MainFragment() {

    override fun getTitleKey(): String = "aboutController.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.home
            identifier = items.count().toLong()
        }
        items += titleItem {
            text = strings["app.name"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = stringsFormat("aboutController.appVersion", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["aboutController.mainMessage.title"]
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["aboutController.mainMessage.subtitle"]
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += dividerItem {
            identifier = items.count().toLong()
        }
        items += linkItem {
            iconRes = R.drawable.ic_about
            text = strings["aboutController.webpage"]
            url = strings["aboutController.webpageUrl"]
            identifier = items.size.toLong()
        }
        items += dividerItem {
            identifier = items.count().toLong()
        }
        items += linkItem {
            iconRes = R.drawable.ic_faq
            text = strings["aboutController.faq"]
            url = strings["aboutController.faqUrl"]
            identifier = items.size.toLong()
        }
        items += dividerItem {
            identifier = items.count().toLong()
        }
        items += linkItem {
            iconRes = R.drawable.ic_feedback
            text = strings["aboutController.opinion"]
            url = strings["aboutController.opinionUrl"]
            identifier = items.size.toLong()
        }

        return items
    }
}