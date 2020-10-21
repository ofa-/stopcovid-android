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
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.openAppSettings
import com.lunabeestudio.stopcovid.coreui.extension.showPermissionRationale
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.doubleButtonItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class ReportFragment : MainFragment() {

    private val args: ReportFragmentArgs by navArgs()
    private var codeUsed: Boolean = false
    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (robertManager.isRegistered) {
            if (args.code != null && !codeUsed) {
                findNavController().safeNavigate(ReportFragmentDirections.actionReportFragmentToCodeFragment(args.code))
                codeUsed = true
            }
        }
    }

    override fun getTitleKey(): String = "declareController.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.declare
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        if (robertManager.isRegistered) {
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
                                requestPermissions(
                                    arrayOf(Manifest.permission.CAMERA),
                                    UiConstants.Permissions.CAMERA.ordinal
                                )
                            }
                        } else {
                            requestPermissions(
                                arrayOf(Manifest.permission.CAMERA),
                                UiConstants.Permissions.CAMERA.ordinal
                            )
                        }
                    } else {
                        findNavController().safeNavigate(ReportFragmentDirections.actionReportFragmentToQrCodeFragment())
                    }
                }
                onClickListener2 = View.OnClickListener {
                    findNavController().safeNavigate(ReportFragmentDirections.actionReportFragmentToCodeFragment())
                }
                identifier = items.count().toLong()
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_xlarge
                identifier = items.count().toLong()
            }
        } else {
            items += titleItem {
                text = strings["declareController.notRegistered.mainMessage.title"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["declareController.notRegistered.mainMessage.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
        }

        return items
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == UiConstants.Permissions.CAMERA.ordinal) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                findNavController().safeNavigate(ReportFragmentDirections.actionReportFragmentToQrCodeFragment())
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