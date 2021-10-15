package com.lunabeestudio.stopcovid.fragment

import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.fastitem.phoneSupportItem
import com.mikepenz.fastadapter.GenericItem

class UrgentInfoFragment : MainFragment() {
    override fun getTitleKey(): String = "dgsUrgentController.title"

    override fun getItems(): List<GenericItem> {

        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += cardWithActionItem {
            mainTitle = strings["dgsUrgentController.section.title"]
            mainBody = strings["dgsUrgentController.section.desc"]
            actions = strings["dgsUrgentController.section.url"]?.let { url ->
                listOf(
                    Action(label = strings["dgsUrgentController.section.labelUrl"]) {
                        context?.let(url::openInExternalBrowser)
                    }
                )
            }
            identifier = "dgsUrgentController.section.title".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        strings["dgsUrgentController.phone.number"]?.let { number ->
            items += phoneSupportItem {
                title = strings["dgsUrgentController.phone.title"]
                subtitle = strings["dgsUrgentController.phone.subtitle"]
                onClick = {
                    context?.let { number.callPhone(it) }
                }
                identifier = "dgsUrgentController.phone.title".hashCode().toLong()
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        return items
    }
}