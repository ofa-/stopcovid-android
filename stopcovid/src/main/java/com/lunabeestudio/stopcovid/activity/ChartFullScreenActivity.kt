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

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsControllerCompat
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.databinding.ActivityChartsFullScreenBinding
import com.lunabeestudio.stopcovid.viewmodel.ChartFullScreenViewModel

class ChartFullScreenActivity : BaseActivity() {

    val viewModel: ChartFullScreenViewModel by viewModels()

    val binding: ActivityChartsFullScreenBinding by lazy {
        ActivityChartsFullScreenBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            this.intent = it
            handleIntent(it)
        }
    }

    private fun handleIntent(intent: Intent) {
        intent.extras?.let { extras ->
            viewModel.chartData.value = ChartFullScreenActivityArgs.fromBundle(extras).chartFullScreenData
        }
    }
}