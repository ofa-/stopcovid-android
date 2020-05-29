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

import android.os.Bundle
import android.view.Menu
import android.view.accessibility.AccessibilityEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.NavMainDirections
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.applyAndConsumeWindowInsetBottom
import com.lunabeestudio.stopcovid.coreui.extension.hideBottomSheet
import com.lunabeestudio.stopcovid.coreui.extension.showSnackBar
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.databinding.ActivityMainBinding
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.fragment.IsSickFragmentDirections

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    private val navController: NavController by lazy {
        supportFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
    }

    private val robertManager: RobertManager by lazy {
        applicationContext.robertManager()
    }

    private var strings: HashMap<String, String> = StringsManager.getStrings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setSupportActionBar(binding.toolbar)
        setupNavigation()

        binding.rootView.applyAndConsumeWindowInsetBottom()
        binding.bottomSheetLayout.bottomSheetFrameLayout.hideBottomSheet()
        binding.toolbar.contentInsetStartWithNavigation = 0

        setContentView(binding.root)

        initStringsObserver()
    }

    private fun initStringsObserver() {
        StringsManager.strings.observe(this) { strings ->
            this.strings = strings
            refreshMenuItemTitles(binding.bottomNavigationView.menu)
            invalidateOptionsMenu()
        }
    }

    private fun setupNavigation() {
        binding.bottomNavigationView.run {
            inflateMenu(R.menu.bottom_menu)

            val appBarActivity = AppBarConfiguration(binding.bottomNavigationView.menu)
            setupActionBarWithNavController(navController, appBarActivity)

            refreshMenuItemTitles(menu)

            navController.addOnDestinationChangedListener { _, destination, _ ->
                val destinationId = menu.children.find {
                    destination.id == it.itemId
                }
                isVisible = destinationId != null
                binding.toolbar.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                if (destinationId != menu.findItem(R.id.proximityFragment)) {
                    binding.errorLayout.isInvisible = true
                }
            }

            NavigationUI.setupWithNavController(this, navController)
        }

        if (robertManager.isSick) {
            navController.navigate(IsSickFragmentDirections.actionGlobalIsSickFragment())
        }
    }

    private fun refreshMenuItemTitles(menu: Menu) {
        menu.findItem(R.id.proximityFragment).title = strings["proximityController.tabBar.title"]
        menu.findItem(R.id.reportFragment).title = strings["sickController.tabBar.title"]
        menu.findItem(R.id.sharingFragment).title = strings["sharingController.tabBar.title"]
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
