package com.lunabeestudio.stopcovid.fragment

import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize

abstract class QRCodeListFragment : MainFragment() {

    protected val barcodeEncoder: BarcodeEncoder = BarcodeEncoder()
    protected val qrCodeSize: Int by lazy {
        R.dimen.qr_code_size.toDimensSize(requireContext()).toInt()
    }

}