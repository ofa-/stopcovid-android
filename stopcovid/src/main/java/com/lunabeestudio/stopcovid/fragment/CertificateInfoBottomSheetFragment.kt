/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lunabeestudio.stopcovid.databinding.FragmentCertificateInfoBottomSheetBinding

class CertificateInfoBottomSheetFragment : BottomSheetDialogFragment() {

    private val args by navArgs<CertificateInfoBottomSheetFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentCertificateInfoBottomSheetBinding.inflate(inflater, container, false)

        val spanText = args.text.toSpannable().also {
            Linkify.addLinks(it, Linkify.WEB_URLS)
        }
        binding.textView.text = spanText

        val textColor = ContextCompat.getColor(binding.root.context, args.headerState.textColor)
        binding.textView.setTextColor(textColor)

        val backgroundColor = ContextCompat.getColor(binding.root.context, args.headerState.backgroundColor)
        binding.walletBottomSheetCard.setCardBackgroundColor(backgroundColor)

        if (!spanText.getSpans(0, spanText.length, URLSpan::class.java).isNullOrEmpty()) {
            binding.textView.movementMethod = LinkMovementMethod.getInstance()
        } else {
            binding.textView.movementMethod = null
        }

        return binding.root
    }
}