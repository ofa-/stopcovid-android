/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/29/10 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.extension.attestationLongLabelFromKey
import com.lunabeestudio.stopcovid.extension.attestationShortLabelFromKey
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.fastitem.selectionItem
import com.lunabeestudio.stopcovid.manager.FormManager
import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModel
import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class NewAttestationPickerFragment : MainFragment() {

    private val viewModel: NewAttestationViewModel by activityViewModels {
        NewAttestationViewModelFactory(requireContext().secureKeystoreDataSource())
    }

    private val args: NewAttestationPickerFragmentArgs by navArgs()

    override fun getTitleKey(): String = "attestation.form.${args.key}.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        if (!strings["attestation.form.${args.key}.header"].isNullOrBlank()) {
            items += captionItem {
                text = strings["attestation.form.${args.key}.header"]
                textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
                identifier = text.hashCode().toLong()
            }
        }

        FormManager.form.value?.peekContent()?.let { form ->
            form.forEach { section ->
                val formEntry = section.firstOrNull { formEntry ->
                    formEntry.key == args.key
                }
                formEntry?.items?.forEach { formEntryItem ->
                    items += selectionItem {
                        title = strings[formEntryItem.code.attestationShortLabelFromKey()]
                        caption = strings[formEntryItem.code.attestationLongLabelFromKey()]
                        showSelection = formEntryItem.code == args.selectedCode
                        onClick = {
                            viewModel.pickFormEntry(args.dataKey, FormEntry(formEntryItem.code, formEntry.type, args.key))
                            findNavControllerOrNull()?.popBackStack()
                        }
                        identifier = title.hashCode().toLong()
                    }
                }
            }
        }
        if (!strings["attestation.form.${args.key}.footer"].isNullOrBlank()) {
            items += captionItem {
                text = strings["attestation.form.${args.key}.footer"]
                textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
                identifier = text.hashCode().toLong()
            }
        }

        return items
    }
}