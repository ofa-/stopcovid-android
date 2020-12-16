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

import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.databinding.LayoutButtonBottomSheetBinding
import com.lunabeestudio.stopcovid.coreui.extension.applyAndConsumeWindowInsetBottom
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.databinding.ActivityOnBoardingBinding
import com.lunabeestudio.stopcovid.databinding.ActivitySplashScreenBinding
import com.lunabeestudio.stopcovid.extension.isOnBoardingDone
import com.lunabeestudio.stopcovid.viewmodel.OnBoardingViewModel
import com.lunabeestudio.stopcovid.viewmodel.OnBoardingViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class OnBoardingActivity : BaseActivity() {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val viewModel: OnBoardingViewModel by viewModels { OnBoardingViewModelFactory() }

    lateinit var splashScreenBinding: ActivitySplashScreenBinding
    lateinit var binding: ActivityOnBoardingBinding
    lateinit var mergeBinding: LayoutButtonBottomSheetBinding
    private var onDestinationChangeListener: NavController.OnDestinationChangedListener? = null

    private val navController: NavController by lazy {
        supportFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
    }

    var splashLoadingJob: Job? = null
    var noStringDialog: AlertDialog? = null

    @OptIn(ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val onBoardingDone = sharedPreferences.isOnBoardingDone

        splashScreenBinding = ActivitySplashScreenBinding.inflate(layoutInflater)

        if (!onBoardingDone) {
            setTheme(R.style.Theme_StopCovid)
            setContentView(splashScreenBinding.root)
        }

        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        mergeBinding = LayoutButtonBottomSheetBinding.bind(binding.root)

        // Wait 2 + 5 seconds to load strings from file or server. Show blocking error if we still don't have strings.
        splashLoadingJob = lifecycleScope.launchWhenResumed {
            delay(2.seconds)
            splashScreenBinding.progressBar.show()
            delay(5.seconds)
            splashScreenBinding.progressBar.hide()
            showNoStringsErrorDialog()
        }

        if (StringsManager.strings.isEmpty()) {
            val stringsObserver = object : Observer<Event<HashMap<String, String>>> {
                override fun onChanged(strings: Event<HashMap<String, String>>?) {
                    if (!strings?.peekContent().isNullOrEmpty()) {
                        StringsManager.liveStrings.removeObserver(this)
                        startOnBoardingOrMain(onBoardingDone, savedInstanceState)
                    }
                }
            }
            StringsManager.liveStrings.observe(this@OnBoardingActivity, stringsObserver)
        } else {
            startOnBoardingOrMain(onBoardingDone, savedInstanceState)
        }
    }

    private fun startOnBoardingOrMain(onBoardingDone: Boolean, savedInstanceState: Bundle?) {
        if (onBoardingDone) {
            val intent = Intent(this@OnBoardingActivity, MainActivity::class.java).apply {
                data = intent.data
            }
            startActivity(
                intent,
                ActivityOptions
                    .makeCustomAnimation(
                        this@OnBoardingActivity,
                        R.anim.nav_default_enter_anim,
                        R.anim.nav_default_exit_anim
                    )
                    .toBundle()
            )
            finishAndRemoveTask()
        } else {
            splashLoadingJob?.cancel("Starting on boarding")
            splashScreenBinding.progressBar.hide()
            noStringDialog?.dismiss()

            setContentView(binding.root)
            setSupportActionBar(binding.toolbar)
            setupActionBarWithNavController(navController)

            onDestinationChangeListener = NavController.OnDestinationChangedListener { _, _, _ ->
                binding.toolbar.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
            }
            onDestinationChangeListener?.let { navController.addOnDestinationChangedListener(it) }

            binding.snackBarView.applyAndConsumeWindowInsetBottom()
            binding.toolbar.contentInsetStartWithNavigation = 0

            if (savedInstanceState == null) {
                replaceSplashScreenLogo()
            } else {
                viewModel.showLogo.value = true
                binding.animationImageView.isVisible = false
            }
        }
    }

    private fun showNoStringsErrorDialog() {
        if (!isFinishing) {
            noStringDialog = MaterialAlertDialogBuilder(this@OnBoardingActivity)
                .setTitle(getString(R.string.splashScreenErrorDialog_title, getString(R.string.app_name)))
                .setMessage(R.string.splashScreenErrorDialog_message)
                .setPositiveButton(R.string.splashScreenErrorDialog_positiveButton) { _, _ ->
                    splashLoadingJob = lifecycleScope.launch {
                        splashScreenBinding.progressBar.show()
                        StringsManager.initialize(this@OnBoardingActivity)
                        StringsManager.onAppForeground(this@OnBoardingActivity)
                        splashScreenBinding.progressBar.hide()
                        if (StringsManager.strings.isEmpty()) {
                            showNoStringsErrorDialog()
                        }
                    }
                }
                .setNegativeButton(R.string.splashScreenErrorDialog_negativeButton) { _, _ ->
                    exitProcess(0)
                }
                .setOnDismissListener {
                    noStringDialog = null
                }
                .show()
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
        onDestinationChangeListener?.let { navController.removeOnDestinationChangedListener(it) }
        super.onDestroy()
    }
}