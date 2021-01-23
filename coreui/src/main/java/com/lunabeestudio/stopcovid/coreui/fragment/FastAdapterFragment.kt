/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.lunabeestudio.stopcovid.coreui.databinding.FragmentRecyclerViewBinding
import com.lunabeestudio.stopcovid.coreui.extension.closeKeyboardOnScroll
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.adapters.GenericFastItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class FastAdapterFragment : BaseFragment() {
    protected var binding: FragmentRecyclerViewBinding? = null
    private var adapter: FastItemAdapter<GenericItem> = GenericFastItemAdapter()
    protected abstract fun getItems(): List<GenericItem>
    protected abstract fun getAppBarLayout(): AppBarLayout?
    private var onScrollListener: RecyclerView.OnScrollListener? = null
    private var refreshScreenJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        adapter.attachDefaultListeners = false
        binding = FragmentRecyclerViewBinding.inflate(inflater, container, false)
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        binding?.recyclerView?.adapter = adapter
        onScrollListener = binding?.recyclerView?.closeKeyboardOnScroll(context)
        return binding?.root
    }

    override fun refreshScreen() {
        refreshScreenJob?.cancel()
        refreshScreenJob = viewLifecycleOwnerOrNull()?.lifecycleScope?.launch(Dispatchers.Main) {
            delay(10)
            val items = getItems()
            if (items.isEmpty()) {
                showEmpty()
            } else {
                adapter.setNewList(items)
                showData()
            }
        }
    }

    protected fun showLoading() {
        binding?.recyclerView?.isVisible = false
        binding?.emptyLayout?.isVisible = false
        binding?.loadingLayout?.isVisible = true
    }

    protected fun showEmpty() {
        binding?.recyclerView?.isVisible = false
        binding?.emptyLayout?.isVisible = true
        binding?.loadingLayout?.isVisible = false
    }

    protected fun showData() {
        binding?.recyclerView?.isVisible = true
        binding?.emptyLayout?.isVisible = false
        binding?.loadingLayout?.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onScrollListener?.let {
            binding?.recyclerView?.removeOnScrollListener(it)
        }
        binding = null
    }
}