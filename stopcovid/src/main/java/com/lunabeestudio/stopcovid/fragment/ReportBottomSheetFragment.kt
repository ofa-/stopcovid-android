package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lunabeestudio.stopcovid.coreui.LocalizedApplication
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.databinding.FragmentReportBottomSheetBinding
import com.lunabeestudio.stopcovid.extension.safeNavigate

class ReportBottomSheetFragment : BottomSheetDialogFragment() {

    private val strings: LocalizedStrings
        get() = (activity?.application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentReportBottomSheetBinding.inflate(inflater, container, false)

        with(binding.codeButton) {
            text = strings["declareController.button.tap"]
            setOnClickListener {
                findNavControllerOrNull()?.safeNavigate(
                    ReportBottomSheetFragmentDirections.actionReportBottomSheetFragmentToCodeFragment()
                )
            }
        }

        with(binding.qrButton) {
            text = strings["declareController.button.flash"]
            setOnClickListener {
                findNavControllerOrNull()?.safeNavigate(
                    ReportBottomSheetFragmentDirections.actionReportBottomSheetFragmentToReportQrCodeFragment()
                )
            }
        }

        with(binding.cancelButton) {
            text = strings["common.cancel"]
            setOnClickListener {
                findNavControllerOrNull()?.popBackStack()
            }
        }
        return binding.root
    }
}
