/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.activity

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.databinding.LayoutButtonBottomSheetBinding
import com.lunabeestudio.stopcovid.coreui.extension.applyAndConsumeWindowInsetBottom
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.databinding.ActivityOnBoardingBinding

class OnBoardingActivity : BaseActivity() {

    lateinit var binding: ActivityOnBoardingBinding
    lateinit var mergeBinding: LayoutButtonBottomSheetBinding
    private var onDestinationChangeListener: NavController.OnDestinationChangedListener? = null

    private val navController: NavController by lazy {
        supportFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        mergeBinding = LayoutButtonBottomSheetBinding.bind(binding.root)

        setContentView(binding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode()
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController)

        onDestinationChangeListener = NavController.OnDestinationChangedListener { _, _, _ ->
            binding.toolbar.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
        onDestinationChangeListener?.let { navController.addOnDestinationChangedListener(it) }

        binding.snackBarView.applyAndConsumeWindowInsetBottom()
        binding.toolbar.contentInsetStartWithNavigation = 0
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    override fun onDestroy() {
        onDestinationChangeListener?.let { navController.removeOnDestinationChangedListener(it) }
        super.onDestroy()
    }
}