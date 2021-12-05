/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/30/09 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lunabeestudio.stopcovid.coreui.LocalizedApplication
import com.lunabeestudio.stopcovid.coreui.databinding.FragmentBottomSheetRecyclerViewBinding
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter

abstract class FastAdapterBottomSheetDialogFragment : BottomSheetDialogFragment() {

    val strings: LocalizedStrings
        get() = (activity?.application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

    val adapter: GenericFastItemAdapter = GenericFastItemAdapter().apply {
        attachDefaultListeners = false
    }

    lateinit var binding: FragmentBottomSheetRecyclerViewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentBottomSheetRecyclerViewBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(context)
        }
        refreshScreen()
    }

    abstract fun refreshScreen()
}