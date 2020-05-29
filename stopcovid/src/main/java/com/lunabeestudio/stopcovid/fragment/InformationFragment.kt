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
import androidx.navigation.fragment.findNavController
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInChromeTab
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.fastitem.iconTitleItem
import com.mikepenz.fastadapter.GenericItem

class InformationFragment : MainFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun getTitleKey(): String = "informationController.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.count().toLong()
        }
        if (robertManager.isAtRisk) {
            items += titleItem {
                text = strings["informationController.mainMessage.atRisk.title"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["informationController.mainMessage.atRisk.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
        } else {
            items += titleItem {
                text = strings["informationController.mainMessage.nothing.title"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["informationController.mainMessage.nothing.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.count().toLong()
        }
        var titleCount = 1
        if (robertManager.isAtRisk) {
            items += titleItem {
                text = stringsFormat("informationController.step.isolate.atRisk.title", titleCount++)
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["informationController.step.isolate.atRisk.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
        } else {
            items += titleItem {
                text = stringsFormat("informationController.step.isolate.nothing.title", titleCount++)
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["informationController.step.isolate.nothing.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
        }
        items += buttonItem {
            text = strings["informationController.step.isolate.buttonTitle"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                findNavController().navigate(InformationFragmentDirections.actionInformationFragmentToGestureFragment())
            }
            identifier = items.count().toLong()
        }
        if (robertManager.isAtRisk) {
            items += titleItem {
                text = stringsFormat("informationController.step.beCareful.atRisk.title", titleCount++)
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["informationController.step.beCareful.atRisk.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
        } else {
            items += titleItem {
                text = stringsFormat("informationController.step.beCareful.nothing.title", titleCount++)
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["informationController.step.beCareful.nothing.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
        }
        items += buttonItem {
            text = strings["informationController.step.beCareful.buttonTitle"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["sickController.button.myConditionInformation.url"]?.openInChromeTab(requireContext())
            }
            identifier = items.count().toLong()
        }
        if (robertManager.isAtRisk) {
            items += titleItem {
                text = stringsFormat("informationController.step.appointment.title", titleCount++)
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["informationController.step.appointment.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += buttonItem {
                text = strings["callCenter.phoneNumber"]
                gravity = Gravity.CENTER
                onClickListener = View.OnClickListener {
                    strings["callCenter.phoneNumber"]?.callPhone(requireContext())
                }
                identifier = items.count().toLong()
            }
        }
        items += titleItem {
            text = stringsFormat("informationController.step.moreInfo.title", titleCount++)
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["informationController.step.moreInfo.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += buttonItem {
            text = strings["informationController.step.moreInfo.buttonTitle"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["informationController.step.moreInfo.url"]?.openInChromeTab(requireContext())
            }
            identifier = items.count().toLong()
        }

        return items
    }
}