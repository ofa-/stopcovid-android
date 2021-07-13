/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/25/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import androidx.navigation.fragment.navArgs
import com.lunabeestudio.domain.model.WalletCertificateError
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.certificateThumbnailDrawable
import com.lunabeestudio.stopcovid.extension.certificateThumbnailFilename
import com.lunabeestudio.stopcovid.extension.errorStringKey
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.phoneSupportItem
import com.lunabeestudio.stopcovid.fastitem.walletSingleDocumentCardItem
import com.mikepenz.fastadapter.GenericItem
import java.io.File

class WalletCertificateErrorFragment : MainFragment() {

    private val args: WalletCertificateErrorFragmentArgs by navArgs()

    override fun getTitleKey(): String = "walletCertificateErrorController.title"

    override fun getItems(): List<GenericItem> {
        val items = mutableListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }

        items += getCertificateItems()

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }

        items += phoneSupportItem {
            title = strings["walletController.phone.title"]
            subtitle = strings["walletController.phone.subtitle"]
            onClick = {
                strings["walletController.phone.number"]?.callPhone(requireContext())
            }
            identifier = title.hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        return items
    }

    private fun getCertificateItems(): MutableList<GenericItem> {
        val items = mutableListOf<GenericItem>()

        items += cardWithActionItem {
            val errorStringKey = args.certificateType.errorStringKey
            when (args.certificateError) {
                WalletCertificateError.INVALID_CERTIFICATE_SIGNATURE -> {
                    mainTitle = strings["walletCertificateErrorController.explanations.invalidSignature.$errorStringKey.title"]
                    mainBody =
                        strings["walletCertificateErrorController.explanations.invalidSignature.$errorStringKey.subtitle"]
                }
                WalletCertificateError.MALFORMED_CERTIFICATE -> {
                    mainTitle = strings["walletCertificateErrorController.explanations.invalidFormat.$errorStringKey.title"]
                    mainBody = strings["walletCertificateErrorController.explanations.invalidFormat.$errorStringKey.subtitle"]
                }
            }

            identifier = mainBody.hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += walletSingleDocumentCardItem {
            mainTitle = strings["walletCertificateErrorController.checkDocument.${args.certificateType.errorStringKey}.title"]
            mainBody = strings["walletCertificateErrorController.checkDocument.${args.certificateType.errorStringKey}.subtitle"]
            onClick = {
                findNavControllerOrNull()
                    ?.safeNavigate(
                        WalletCertificateErrorFragmentDirections
                            .actionWalletAddCertificateFragmentToCertificateDocumentExplanationFragment(args.certificateType)
                    )
            }
            certificateFile = File(requireContext().filesDir, args.certificateType.certificateThumbnailFilename)
            certificateDrawable = args.certificateType.certificateThumbnailDrawable
            identifier = mainBody.hashCode().toLong()
        }

        return items
    }
}