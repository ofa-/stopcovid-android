/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/12 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.privateEventQrCode
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.fastitem.privateVenueQrCodeItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.manager.VenuesManager
import com.mikepenz.fastadapter.GenericItem

class VenuesPrivateEventFragment : MainFragment() {

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val barcodeEncoder = BarcodeEncoder()
    private val qrCodeSize by lazy {
        R.dimen.qr_code_size.toDimensSize(requireContext()).toInt()
    }
    private lateinit var bitmap: Bitmap

    override fun getTitleKey(): String = "venuesPrivateEventController.title"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VenuesManager.generateNewQRCodeIfNeeded(sharedPrefs, requireContext().robertManager(), requireContext().secureKeystoreDataSource())
        bitmap = barcodeEncoder.encodeBitmap(
            sharedPrefs.privateEventQrCode,
            BarcodeFormat.QR_CODE,
            qrCodeSize,
            qrCodeSize
        )
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += privateVenueQrCodeItem {
            qrCodeBitmap = bitmap
            text = sharedPrefs.privateEventQrCode
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["venuesPrivateEventController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = "venuesPrivateEventController.mainMessage.title".hashCode().toLong()
        }
        items += captionItem {
            text = strings["venuesPrivateEventController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = "venuesPrivateEventController.mainMessage.subtitle".hashCode().toLong()
        }
        items += buttonItem {
            text = strings["venuesPrivateEventController.button.sharedCode"]
            width = ViewGroup.LayoutParams.MATCH_PARENT
            onClickListener = View.OnClickListener {
                val uri = ShareManager.getShareCaptureUriFromBitmap(requireContext(), bitmap, "qrCode")
                val text = listOf(
                    strings["venuesPrivateEventController.sharing.text"],
                    sharedPrefs.privateEventQrCode,
                ).joinToString(" ")
                ShareManager.shareImageAndText(requireContext(), uri, text) {
                    strings["common.error.unknown"]?.let { showErrorSnackBar(it) }
                }
            }
            identifier = "venuesPrivateEventController.button.sharedCode".hashCode().toLong()
        }

        return items
    }
}