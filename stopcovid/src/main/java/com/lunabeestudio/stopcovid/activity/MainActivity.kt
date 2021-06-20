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
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.core.view.isInvisible
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.applyAndConsumeWindowInsetBottom
import com.lunabeestudio.stopcovid.coreui.extension.showSnackBar
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.databinding.ActivityMainBinding
import com.lunabeestudio.stopcovid.extension.alertRiskLevelChanged
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.showAlertRiskLevelChanged
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.manager.RisksLevelManager

class MainActivity : BaseActivity() {

    lateinit var binding: ActivityMainBinding

    private val navController: NavController by lazy {
        supportFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private var strings: LocalizedStrings = StringsManager.strings

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setSupportActionBar(binding.toolbar)
        setupNavigation()

        binding.snackBarView.applyAndConsumeWindowInsetBottom()
        binding.toolbar.contentInsetStartWithNavigation = 0

        setContentView(binding.root)
        // invisible in the xml is not working
        binding.errorLayout.isInvisible = true

        initStringsObserver()
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (sharedPrefs.alertRiskLevelChanged) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork(Constants.WorkerNames.AT_RISK_NOTIFICATION)

            MaterialAlertDialogBuilder(this).showAlertRiskLevelChanged(
                strings,
                sharedPrefs,
                RisksLevelManager.getCurrentLevel(robertManager().atRiskStatus?.riskLevel),
            )
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val extraData = intent.getStringExtra("data")?.let {
                Uri.parse(it)
            }

            fixIntentData(intent)

            (extraData ?: intent.data)?.let { data ->
                if (navController.graph.hasDeepLink(data)) {
                    navController.navigate(
                        data,
                        navOptions {
                            anim {
                                enter = R.anim.nav_default_enter_anim
                                exit = R.anim.nav_default_exit_anim
                                popEnter = R.anim.nav_default_pop_enter_anim
                                popExit = R.anim.nav_default_pop_exit_anim
                            }
                            launchSingleTop = true
                        }
                    )
                }
            }

            intent.data = null
        }
    }

    private fun fixIntentData(intent: Intent) {
        // since we use '#' to separate the code, we replace it to pass a clean URL to the nav_graph deeplink handle
        val uri = intent.data?.let {
            DeeplinkManager.transformFragmentToCodeParam(it)
        }
        val uriBuilder = uri?.buildUpon()
            ?.path(intent.data?.path?.takeIf { it != "/" })

        intent.data = uriBuilder?.build()
    }

    private fun initStringsObserver() {
        StringsManager.liveStrings.observeEventAndConsume(this) { strings ->
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
