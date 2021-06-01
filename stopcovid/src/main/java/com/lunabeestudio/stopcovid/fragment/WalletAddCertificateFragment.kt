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

import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.fastitem.addCertificateCardItem
import com.lunabeestudio.stopcovid.fastitem.phoneSupportItem
import com.mikepenz.fastadapter.GenericItem
import java.io.File

class WalletAddCertificateFragment : MainFragment() {
    override fun getTitleKey(): String = "walletController.addCertificate"

    override fun getItems(): List<GenericItem> {
        val items = mutableListOf<GenericItem>()

        val parentFile = requireContext().filesDir

        items += addCertificateCardItem {
            title = strings["walletAddCertificateController.testCertificate.title"]
            subtitle = strings["walletAddCertificateController.testCertificate.subtitle"]
            imageFile = File(parentFile, ConfigConstant.Wallet.TEST_CERTIFICATE_THUMBNAIL_FILE)
            fallbackRes = R.drawable.test_certificate
            mainAction = {
                findNavControllerOrNull()
                    ?.navigate(
                        WalletAddCertificateFragmentDirections.actionWalletAddCertificateFragmentToWalletQRFragment(
                            WalletCertificateType.SANITARY
                        )
                    )
            }
            thumbnailAction = {
                findNavControllerOrNull()
                    ?.navigate(WalletAddCertificateFragmentDirections.actionWalletAddCertificateFragmentToTestDocumentExplanationFragment())
            }
            identifier = title.hashCode().toLong()
        }

        items += addCertificateCardItem {
            title = strings["walletAddCertificateController.vaccinCertificate.title"]
            subtitle = strings["walletAddCertificateController.vaccinCertificate.subtitle"]
            imageFile = File(parentFile, ConfigConstant.Wallet.VACCIN_CERTIFICATE_THUMBNAIL_FILE)
            fallbackRes = R.drawable.vaccin_certificate
            mainAction = {
                findNavControllerOrNull()
                    ?.navigate(
                        WalletAddCertificateFragmentDirections.actionWalletAddCertificateFragmentToWalletQRFragment(
                            WalletCertificateType.VACCINATION
                        )
                    )
            }
            thumbnailAction = {
                findNavControllerOrNull()
                    ?.navigate(WalletAddCertificateFragmentDirections.actionWalletAddCertificateFragmentToVaccinDocumentExplanationFragment())
            }
            identifier = title.hashCode().toLong()
        }

        items += captionItem {
            text = strings["walletController.addCertificate.explanation"]
            textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
            identifier = text.hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
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

        return items
    }
}