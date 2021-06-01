/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/20/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentDocumentExplanationBinding

abstract class DocumentExplanationFragment : BaseFragment() {

    protected lateinit var binding: FragmentDocumentExplanationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDocumentExplanationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun refreshScreen() {
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = false
    }
}