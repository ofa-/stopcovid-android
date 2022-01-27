/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/30/09 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fragment.FastAdapterBottomSheetDialogFragment
import com.lunabeestudio.stopcovid.fastitem.selectionItem
import com.mikepenz.fastadapter.GenericItem

class ChooseMultipassProfileBottomSheetDialogFragment : FastAdapterBottomSheetDialogFragment() {

    private val args by navArgs<ChooseMultipassProfileBottomSheetDialogFragmentArgs>()

    override fun refreshScreen() {
        val items = arrayListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["multiPass.tab.generation.profileList.title"]
            identifier = "multiPass.tab.generation.profileList.title".hashCode().toLong()
        }

        args.selectionData.forEach { data ->
            items += selectionItem {
                title = data.displayText
                showSelection = false
                onClick = {
                    setFragmentResult(
                        CHOOSE_MULTIPASS_PROFILE_RESULT_KEY,
                        bundleOf(CHOOSE_MULTIPASS_PROFILE_BUNDLE_KEY_ID_SELECTED to data.id)
                    )
                    dismiss()
                }
                identifier = data.id.hashCode().toLong()
            }
        }

        adapter.setNewList(items)
    }

    companion object {
        const val CHOOSE_MULTIPASS_PROFILE_RESULT_KEY: String = "CHOOSE_MULTIPASS_PROFILE_RESULT_KEY"
        const val CHOOSE_MULTIPASS_PROFILE_BUNDLE_KEY_ID_SELECTED: String = "CHOOSE_MULTIPASS_PROFILE_BUNDLE_KEY_ID_SELECTED"
    }
}
