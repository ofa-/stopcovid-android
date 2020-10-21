/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.databinding.LayoutButtonBottomSheetBinding
import com.lunabeestudio.stopcovid.coreui.extension.applyAndConsumeWindowInsetBottom
import com.lunabeestudio.stopcovid.databinding.ActivityOnBoardingBinding
import com.lunabeestudio.stopcovid.extension.isOnBoardingDone
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fragment.OnBoardingWelcomeFragmentDirections
import com.lunabeestudio.stopcovid.viewmodel.OnBoardingViewModel
import com.lunabeestudio.stopcovid.viewmodel.OnBoardingViewModelFactory

class OnBoardingActivity : BaseActivity() {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val viewModel: OnBoardingViewModel by viewModels { OnBoardingViewModelFactory() }

    lateinit var binding: ActivityOnBoardingBinding
    lateinit var mergeBinding: LayoutButtonBottomSheetBinding
    private lateinit var onDestinationChangeListener: NavController.OnDestinationChangedListener

    private val navController: NavController by lazy {
        supportFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_StopCovid)
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        mergeBinding = LayoutButtonBottomSheetBinding.bind(binding.root)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController)
        onDestinationChangeListener = NavController.OnDestinationChangedListener { _, _, _ ->
            binding.toolbar.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }
        navController.addOnDestinationChangedListener(onDestinationChangeListener)

        binding.snackBarView.applyAndConsumeWindowInsetBottom()
        binding.toolbar.contentInsetStartWithNavigation = 0

        when {
            sharedPreferences.isOnBoardingDone() -> {
                navController.safeNavigate(OnBoardingWelcomeFragmentDirections.actionOnBoardingWelcomeFragmentToMainActivity(intent.data?.toString()))
                finishAndRemoveTask()
            }
            savedInstanceState == null -> {
                replaceSplashScreenLogo()
            }
            else -> {
                viewModel.showLogo.value = true
                binding.animationImageView.isVisible = false
            }
        }
    }

    private fun replaceSplashScreenLogo() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.animationImageView) { view, insets ->
            view.translationY = binding.snackBarView.paddingBottom.toFloat() / 2f
            animateLogo()
            ViewCompat.setOnApplyWindowInsetsListener(binding.animationImageView, null)
            insets
        }
    }

    private fun animateLogo() {
        binding.animationEndImageView.post {
            binding.toolbar.animate().apply {
                binding.animationImageView.animate().apply {
                    val scale = binding.animationEndImageView.height.toFloat() / binding.animationImageView.height.toFloat()
                    scaleX(scale)
                    scaleY(scale)
                    y(
                        binding.animationEndImageView.y
                            + ((binding.animationEndImageView.height.toFloat() - binding.animationImageView.height.toFloat()) / 2f)
                    )
                    duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                    interpolator = DecelerateInterpolator()
                    withEndAction {
                        viewModel.showLogo.value = true
                        binding.animationImageView.postDelayed({
                            binding.animationImageView.isVisible = false
                        }, resources.getInteger(android.R.integer.config_mediumAnimTime).toLong())
                    }
                    start()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        navController.removeOnDestinationChangedListener(onDestinationChangeListener)
    }
}