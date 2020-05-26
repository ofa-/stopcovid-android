/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.lunabeestudio.stopcovid.coreui.databinding.FragmentRecyclerViewBinding
import com.lunabeestudio.stopcovid.coreui.extension.closeKeyboardOnScroll
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter

abstract class FastAdapterFragment : BaseFragment() {
    protected var binding: FragmentRecyclerViewBinding? = null
    private var adapter: FastItemAdapter<GenericItem> = GenericFastItemAdapter()
    protected abstract fun getItems(): List<GenericItem>
    protected abstract fun getAppBarLayout(): AppBarLayout?
    private var onScrollListener: RecyclerView.OnScrollListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecyclerViewBinding.inflate(inflater, container, false)
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        binding?.recyclerView?.adapter = adapter
        onScrollListener = binding?.recyclerView?.closeKeyboardOnScroll(context)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshScreen()
    }

    override fun refreshScreen() {
        adapter.setNewList(getItems())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onScrollListener?.let {
            binding?.recyclerView?.removeOnScrollListener(it)
        }
        binding = null
    }
}