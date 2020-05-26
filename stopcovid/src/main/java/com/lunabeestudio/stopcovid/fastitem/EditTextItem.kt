/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.BaseItem

class EditTextItem : BaseItem<EditTextItem.ViewHolder>(
    R.layout.item_edit_text, ::ViewHolder, R.id.item_edit_text
) {
    var text: String? = null
    var textInputType: Int = EditorInfo.TYPE_CLASS_TEXT
    var hint: String? = null
    var requestFocus: Boolean = false
    var onTextChange: ((text: Editable?) -> Unit)? = null
    var onDone: (() -> Unit)? = null

    private var textWatcher: TextWatcher? = null

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        initEditText(holder)
        initLayout(holder)
        onTextChange?.let { listener ->
            textWatcher = holder.editText.doAfterTextChanged(listener)
        }
    }

    private fun initEditText(holder: ViewHolder) {
        holder.editText.setText(text)
        holder.editText.inputType = textInputType
        if (requestFocus) {
            holder.editText.requestFocus()
        }
        holder.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onDone?.invoke()
                true
            } else {
                false
            }
        }
    }

    private fun initLayout(holder: ViewHolder) {
        holder.textInputLayout.hint = hint
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        textWatcher?.let {
            holder.editText.removeTextChangedListener(textWatcher)
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val textInputLayout: TextInputLayout = v.findViewById(R.id.textInputLayout)
        val editText: TextInputEditText = v.findViewById(R.id.editText)
    }
}

fun editTextItem(block: (EditTextItem.() -> Unit)): EditTextItem = EditTextItem().apply(block)