/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/09/13 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.activity

import android.os.Bundle
import androidx.core.view.WindowInsetsControllerCompat
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.databinding.ActivityImportQrCodeBinding

class ImportQRCodeActivity : BaseActivity() {

    val binding: ActivityImportQrCodeBinding by lazy {
        ActivityImportQrCodeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode()
    }

    fun showProgress(show: Boolean) {
        if (show) {
            binding.blockingProgressBar.show()
        } else {
            binding.blockingProgressBar.hide()
        }
    }

    companion object {
        const val EXTRA_CODE_SCANNED: String = "Extra.Code.Scanned"
        const val RESULT_KO: Int = 2
    }
}