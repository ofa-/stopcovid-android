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

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.hideSoftKeyBoard
import com.lunabeestudio.stopcovid.coreui.extension.showSoftKeyBoard
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.editTextItem
import com.mikepenz.fastadapter.GenericItem
import java.lang.ref.WeakReference

class CodeFragment : MainFragment() {

    override fun getTitleKey(): String = "enterCodeController.title"

    private var materialButton: WeakReference<MaterialButton>? = null
    private var code: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        code = savedInstanceState?.getString(SAVE_INSTANCE_CODE, null) ?: ""
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        activity?.showSoftKeyBoard()
    }

    override fun onPause() {
        super.onPause()
        activity?.hideSoftKeyBoard()
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.count().toLong()
        }
        items += titleItem {
            text = strings["enterCodeController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["enterCodeController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
        items += editTextItem {
            hint = strings["enterCodeController.textField.placeholder"]
            text = code
            requestFocus = code.isBlank()
            onTextChange = { text ->
                code = text?.toString() ?: ""
                materialButton?.get()?.isEnabled = code.isNotBlank()
            }
            onDone = {
                verifyCode()
            }
            identifier = items.count().toLong()
        }
        items += buttonItem {
            text = strings["enterCodeController.button.validate"]
            gravity = Gravity.CENTER
            isButtonEnabled = code.isNotBlank()
            getMaterialButton = { button ->
                materialButton = WeakReference(button)
            }
            onClickListener = View.OnClickListener {
                verifyCode()
            }
            identifier = items.count().toLong()
        }

        return items
    }

    private fun verifyCode() {
        if (!Constants.ServerConstant.ACCEPTED_REPORT_CODE_LENGTH.contains(code.length)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(strings["enterCodeController.alert.invalidCode.title"])
                .setMessage(strings["enterCodeController.alert.invalidCode.message"])
                .setPositiveButton(strings["common.ok"], null)
                .show()
        } else {
            findNavController().navigate(CodeFragmentDirections.actionCodeFragmentToSymptomsOriginFragment(code))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SAVE_INSTANCE_CODE, code)
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val SAVE_INSTANCE_CODE: String = "Save.Instance.Code"
    }
}