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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.isInvisible
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.databinding.LayoutButtonBottomSheetBinding
import com.lunabeestudio.stopcovid.coreui.extension.applyAndConsumeWindowInsetBottom
import com.lunabeestudio.stopcovid.coreui.extension.hideBottomSheet
import com.lunabeestudio.stopcovid.coreui.extension.showSnackBar
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.databinding.ActivityMainBinding
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fragment.IsSickFragmentDirections

class MainActivity : BaseActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var mergeBinding: LayoutButtonBottomSheetBinding

    private val navController: NavController by lazy {
        supportFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
    }

    private val robertManager: RobertManager by lazy {
        applicationContext.robertManager()
    }

    private var strings: HashMap<String, String> = StringsManager.strings

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        mergeBinding = LayoutButtonBottomSheetBinding.bind(binding.rootView)

        setSupportActionBar(binding.toolbar)
        setupNavigation()

        binding.rootView.applyAndConsumeWindowInsetBottom()
        mergeBinding.bottomSheetFrameLayout.hideBottomSheet()
        binding.toolbar.contentInsetStartWithNavigation = 0

        setContentView(binding.root)
        // invisible in the xml is not working
        binding.errorLayout.isInvisible = true

        initStringsObserver()
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("data")?.let { data ->
            Uri.parse(data)?.let {
                navController.navigate(Uri.parse(data), navOptions {
                    anim {
                        enter = R.anim.nav_default_enter_anim
                        exit = R.anim.nav_default_exit_anim
                        popEnter = R.anim.nav_default_pop_enter_anim
                        popExit = R.anim.nav_default_pop_exit_anim
                    }
                })
            }

            intent.data = null
            setIntent(intent)
        }
    }

    private fun initStringsObserver() {
        StringsManager.liveStrings.observe(this) { strings ->
            this.strings = strings
            invalidateOptionsMenu()
        }
    }

    private fun setupNavigation() {
        setupActionBarWithNavController(this, navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
            binding.errorLayout.isInvisible = destination.id != R.id.proximityFragment
        }

        if (robertManager.isSick) {
            navController.safeNavigate(IsSickFragmentDirections.actionGlobalIsSickFragment())
        }
    }

    fun showSnackBar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        binding.snackBarView.showSnackBar(message, duration)
    }

    fun showErrorSnackBar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        binding.snackBarView.showSnackBar(message, duration, errorSnackBar = true)
    }

    fun showProgress(show: Boolean) {
        if (show) {
            binding.blockingProgressBar.show()
        } else {
            binding.blockingProgressBar.hide()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}