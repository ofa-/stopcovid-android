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
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
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

        items += when (args.certificateType) {
            WalletCertificateType.SANITARY -> getTestItems()
            WalletCertificateType.VACCINATION -> getVaccinItems()
        }

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

    private fun getTestItems(): MutableList<GenericItem> {
        val items = mutableListOf<GenericItem>()

        items += cardWithActionItem {
            when (args.certificateError) {
                WalletCertificateError.INVALID_CERTIFICATE_SIGNATURE -> {
                    mainTitle = strings["walletCertificateErrorController.explanations.invalidSignature.testCertificate.title"]
                    mainBody = strings["walletCertificateErrorController.explanations.invalidSignature.testCertificate.subtitle"]
                }
                WalletCertificateError.MALFORMED_CERTIFICATE -> {
                    mainTitle = strings["walletCertificateErrorController.explanations.invalidFormat.testCertificate.title"]
                    mainBody = strings["walletCertificateErrorController.explanations.invalidFormat.testCertificate.subtitle"]
                }
            }

            identifier = mainBody.hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += walletSingleDocumentCardItem {
            mainTitle = strings["walletCertificateErrorController.checkDocument.testCertificate.title"]
            mainBody = strings["walletCertificateErrorController.checkDocument.testCertificate.subtitle"]
            onClick = {
                findNavControllerOrNull()
                    ?.safeNavigate(
                        WalletCertificateErrorFragmentDirections
                            .actionWalletAddCertificateFragmentToTestDocumentExplanationFragment()
                    )
            }
            certificateFile = File(requireContext().filesDir, ConfigConstant.Wallet.TEST_CERTIFICATE_THUMBNAIL_FILE)
            identifier = mainBody.hashCode().toLong()
        }

        return items
    }

    private fun getVaccinItems(): MutableList<GenericItem> {
        val items = mutableListOf<GenericItem>()

        items += cardWithActionItem {
            when (args.certificateError) {
                WalletCertificateError.INVALID_CERTIFICATE_SIGNATURE -> {
                    mainTitle = strings["walletCertificateErrorController.explanations.invalidSignature.vaccinCertificate.title"]
                    mainBody = strings["walletCertificateErrorController.explanations.invalidSignature.vaccinCertificate.subtitle"]
                }
                WalletCertificateError.MALFORMED_CERTIFICATE -> {
                    mainTitle = strings["walletCertificateErrorController.explanations.invalidFormat.vaccinCertificate.title"]
                    mainBody = strings["walletCertificateErrorController.explanations.invalidFormat.vaccinCertificate.subtitle"]
                }
            }
            identifier = mainBody.hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += walletSingleDocumentCardItem {
            mainTitle = strings["walletCertificateErrorController.checkDocument.vaccinCertificate.title"]
            mainBody = strings["walletCertificateErrorController.checkDocument.vaccinCertificate.subtitle"]
            onClick = {
                findNavControllerOrNull()
                    ?.safeNavigate(
                        WalletCertificateErrorFragmentDirections
                            .actionWalletAddCertificateFragmentToVaccinDocumentExplanationFragment()
                    )
            }
            certificateFile = File(requireContext().filesDir, ConfigConstant.Wallet.VACCIN_CERTIFICATE_THUMBNAIL_FILE)
        }

        return items
    }
}