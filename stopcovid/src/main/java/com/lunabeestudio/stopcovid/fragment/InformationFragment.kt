/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.mikepenz.fastadapter.GenericItem

class InformationFragment : MainFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        robertManager.atRiskStatus.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override fun getTitleKey(): String = "informationController.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.count().toLong()
        }

        val isAtRisk = robertManager.isAtRisk
        val isWarningAtRisk = robertManager.isWarningAtRisk

        addMainMessage(items, isAtRisk, isWarningAtRisk)
        addStep1(items, isAtRisk, isWarningAtRisk)
        addStep2(items, isAtRisk, isWarningAtRisk)
        addStep3(items, isAtRisk, isWarningAtRisk)

        return items
    }

    private fun addMainMessage(items: ArrayList<GenericItem>, isAtRisk: Boolean?, isWarningAtRisk: Boolean?) {
        when {
            isAtRisk == true -> {
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
            }
            isWarningAtRisk == true -> {
                items += titleItem {
                    text = strings["informationController.mainMessage.warning.title"]
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["informationController.mainMessage.warning.subtitle"]
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
            }
            else -> {
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
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.count().toLong()
        }
    }

    private fun addStep1(items: ArrayList<GenericItem>, isAtRisk: Boolean?, isWarningAtRisk: Boolean?) {
        when {
            isAtRisk == true -> {
                items += titleItem {
                    text = stringsFormat("informationController.step.isolate.atRisk.title", 1)
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["informationController.step.isolate.atRisk.subtitle"]
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
            }
            isWarningAtRisk == true -> {
                items += titleItem {
                    text = stringsFormat("informationController.step.isolate.warning.title", 1)
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["informationController.step.isolate.warning.subtitle"]
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
            }
            else -> {
                items += titleItem {
                    text = stringsFormat("informationController.step.isolate.nothing.title", 1)
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["informationController.step.isolate.nothing.subtitle"]
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
            }
        }
        items += buttonItem {
            text = strings["informationController.step.isolate.buttonTitle"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.safeNavigate(InformationFragmentDirections.actionInformationFragmentToGestureFragment())
            }
            identifier = items.count().toLong()
        }
    }

    private fun addStep2(items: ArrayList<GenericItem>, isAtRisk: Boolean?, isWarningAtRisk: Boolean?) {
        when {
            isAtRisk == true -> {
                items += titleItem {
                    text = stringsFormat("informationController.step.beCareful.atRisk.title", 2)
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["informationController.step.beCareful.atRisk.subtitle"]
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
            }
            isWarningAtRisk == true -> {
                items += titleItem {
                    text = stringsFormat("informationController.step.beCareful.warning.title", 2)
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["informationController.step.beCareful.warning.subtitle"]
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
            }
            else -> {
                items += titleItem {
                    text = stringsFormat("informationController.step.beCareful.nothing.title", 2)
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["informationController.step.beCareful.nothing.subtitle"]
                    gravity = Gravity.CENTER
                    identifier = items.count().toLong()
                }
            }
        }
        items += buttonItem {
            text = strings["informationController.step.beCareful.buttonTitle"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["sickController.button.myConditionInformation.url"]?.openInExternalBrowser(requireContext())
            }
            identifier = items.count().toLong()
        }
    }

    private fun addStep3(items: ArrayList<GenericItem>, isAtRisk: Boolean?, isWarningAtRisk: Boolean?) {
        if (isAtRisk == true || isWarningAtRisk == true) {
            items += titleItem {
                text = stringsFormat("informationController.step.appointment.title", 3)
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
    }
}