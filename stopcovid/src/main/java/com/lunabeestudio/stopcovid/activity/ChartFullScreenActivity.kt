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
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.databinding.ActivityChartsFullScreenBinding
import com.lunabeestudio.stopcovid.fragment.ChartFullScreenFragmentArgs

class ChartFullScreenActivity : BaseActivity() {

    val binding: ActivityChartsFullScreenBinding by lazy {
        ActivityChartsFullScreenBinding.inflate(layoutInflater)
    }

    private val args: ChartFullScreenActivityArgs by navArgs()

    private val navController: NavController by lazy {
        supportFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode()

        intent.extras?.let {
            val fragmentArgs = ChartFullScreenFragmentArgs(
                keyFigureKey = args.keyFiguresKey,
                chartDataType = args.chartDataType,
                minDate = args.minDate,
            ).toBundle()
            navController.setGraph(R.navigation.nav_chart_full_screen, fragmentArgs)
        }
    }
}