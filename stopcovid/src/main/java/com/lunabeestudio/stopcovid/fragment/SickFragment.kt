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

import android.Manifest
import android.content.pm.PackageManager
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.extension.openAppSettings
import com.lunabeestudio.stopcovid.coreui.extension.showPermissionRationale
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.lightButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInChromeTab
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.fastitem.contactItem
import com.lunabeestudio.stopcovid.fastitem.doubleButtonItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SickFragment : AboutMainFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun getTitleKey(): String = if (robertManager.isSick) {
        "sickController.sick.title"
    } else {
        "sickController.title"
    }

    private val dateFormat: DateFormat = SimpleDateFormat("dd LLLL", Locale.getDefault())

    override fun getItems(): List<GenericItem> {
        return if (robertManager.isSick) {
            sickItems()
        } else {
            notSickItems()
        }
    }

    private fun sickItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.sick
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["sickController.sick.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["sickController.sick.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += lightButtonItem {
            text = strings["sickController.button.recommendations"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["sickController.button.recommendations.url"]?.openInChromeTab(requireContext())
            }
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["informationController.step.appointment.buttonTitle"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["callCenter.phoneNumber"]?.callPhone(requireContext())
            }
            identifier = items.count().toLong()
        }

        return items
    }

    private fun notSickItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.diagnosis
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        if (robertManager.isAtRisk) {
            val exposureCalendar = Calendar.getInstance()
            exposureCalendar.add(Calendar.DAY_OF_YEAR, -robertManager.lastExposureTimeframe)
            val exposureDate = exposureCalendar.time
            val endExposureCalendar = Calendar.getInstance()
            endExposureCalendar.add(Calendar.DAY_OF_YEAR, 14)
            val endExposureDate = endExposureCalendar.time
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
            items += contactItem {
                header = stringsFormat("sickController.state.date", dateFormat.format(exposureDate))
                title = strings["sickController.state.contact.title"]
                caption = stringsFormat("sickController.state.contact.subtitle", dateFormat.format(endExposureDate))
                more = strings["common.readMore"]
                moreClickListener = View.OnClickListener {
                    findNavController().navigate(SickFragmentDirections.actionSickFragmentToInformationFragment())
                }
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
        }
        items += titleItem {
            text = strings["sickController.message.testedPositive.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["sickController.message.testedPositive.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += doubleButtonItem {
            text1 = strings["sickController.button.flash"]
            text2 = strings["sickController.button.tap"]
            onClickListener1 = View.OnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        context?.showPermissionRationale(strings, "common.needCameraAccessToScan", "common.ok") {
                            requestPermissions(arrayOf(Manifest.permission.CAMERA),
                                UiConstants.Permissions.CAMERA.ordinal)
                        }
                    } else {
                        requestPermissions(arrayOf(Manifest.permission.CAMERA),
                            UiConstants.Permissions.CAMERA.ordinal)
                    }
                } else {
                    findNavController().navigate(SickFragmentDirections.actionSickFragmentToQrCodeFragment())
                }
            }
            onClickListener2 = View.OnClickListener {
                findNavController().navigate(SickFragmentDirections.actionSickFragmentToCodeFragment())
            }
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.count().toLong()
        }

        return items
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == UiConstants.Permissions.CAMERA.ordinal) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                findNavController().navigate(SickFragmentDirections.actionSickFragmentToQrCodeFragment())
            } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                context?.showPermissionRationale(strings, "common.needCameraAccessToScan", "common.settings") {
                    openAppSettings()
                }
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
}