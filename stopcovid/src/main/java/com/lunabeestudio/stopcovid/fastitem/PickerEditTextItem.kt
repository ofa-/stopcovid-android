/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.hideSoftKeyBoard
import com.lunabeestudio.stopcovid.databinding.ItemEditTextBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class PickerEditTextItem : AbstractBindingItem<ItemEditTextBinding>() {
    var text: String? = null
    var hint: String? = null
    var placeholder: String? = null
    var onClick: (() -> Unit)? = null

    override val type: Int = R.id.item_picker_edit_text

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemEditTextBinding {
        return ItemEditTextBinding.inflate(inflater, parent, false)
    }

    override fun bindView(binding: ItemEditTextBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)
        initEditText(binding)
        initLayout(binding)
    }

    private fun initEditText(binding: ItemEditTextBinding) {
        binding.editText.setText(text)
        // Catch the touch event on the textInputEditText and disable its normal behaviour
        binding.editText.isFocusableInTouchMode = false
        binding.editText.isFocusable = false
        binding.editText.isLongClickable = false
        binding.editText.setOnClickListener {
            (binding.editText.context as? Activity)?.hideSoftKeyBoard()
            onClick?.invoke()
        }
    }

    private fun initLayout(binding: ItemEditTextBinding) {
        binding.textInputLayout.hint = hint
        binding.textInputLayout.placeholderText = placeholder
    }
}

fun pickerEditTextItem(block: (PickerEditTextItem.() -> Unit)): PickerEditTextItem = PickerEditTextItem().apply(block)