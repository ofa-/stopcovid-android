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
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.applyAndConsumeWindowInsetBottom
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.showSnackBar
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.databinding.ActivityMainBinding
import com.lunabeestudio.stopcovid.extension.alertRiskLevelChanged
import com.lunabeestudio.stopcovid.extension.isLaunchedFromHistory
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.showAlertRiskLevelChanged
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.manager.RisksLevelManager
import kotlinx.coroutines.delay

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
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode()

        // invisible in the xml is not working
        binding.errorLayout.isInvisible = true

        initStringsObserver()

        if (intent?.isLaunchedFromHistory == false) {
            handleIntent(intent)
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            setupAppShortcuts()
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val extraData = intent.getStringExtra("data")?.let {
                Uri.parse(it)
            }

            fixIntentData(intent)

            (extraData ?: intent.data)?.let { data ->
                processDeeplink(data)
            }

            intent.data = null
        }
    }

    fun processDeeplink(data: Uri) {
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
            if (destination.id != R.id.proximityFragment) {
                binding.errorLayout.isInvisible = true
            }
            refreshAppBarLayout(destination)
        }
    }

    private fun refreshAppBarLayout(destination: NavDestination) {
        listOf(
            R.id.reportQrCodeFragment,
            R.id.venueQrCodeFragment,
            R.id.walletQRCodeFragment,
            R.id.universalQrScanFragment,
            R.id.verifyWalletQRCodeFragment,
        ).contains(destination.id).let { isQrCodeFragment ->
            lifecycleScope.launchWhenResumed {
                val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
                if (isQrCodeFragment) {
                    // wait for default fragment switch animation time
                    delay(200L)

                    windowInsetsController.isAppearanceLightStatusBars = false
                    binding.appBarLayout.isVisible = false

                    // Fix issue where appBarLayout take space even when gone
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = null
                    binding.navHostFragment.requestLayout()
                } else {
                    setSupportActionBar(binding.toolbar)
                    setupActionBarWithNavController(this@MainActivity, navController)
                    // wait for default fragment switch animation time
                    delay(200L)

                    windowInsetsController.isAppearanceLightStatusBars = !isNightMode()
                    binding.appBarLayout.isVisible = true

                    // Fix issue where appBarLayout take space even when gone
                    val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                    params.behavior = AppBarLayout.ScrollingViewBehavior()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun setupAppShortcuts() {
        ContextCompat.getSystemService(this, ShortcutManager::class.java)?.let { shortcutManager ->
            val curfewCertificateShortcut = createCurfewCertificateShortcut()
            val universalQrCodeShortcut = createUniversalQrCodeShortcut()

            shortcutManager.setDynamicShortcuts(listOfNotNull(curfewCertificateShortcut, universalQrCodeShortcut))
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createCurfewCertificateShortcut(): ShortcutInfo? {
        return if (robertManager().configuration.displayAttestation) {
            val builder = ShortcutInfo.Builder(this, CURFEW_CERTIFICATE_SHORTCUT_ID)

            builder.setShortLabel(strings["attestationsController.title"] ?: "Attestations")
            builder.setLongLabel(strings["home.moreSection.curfewCertificate"] ?: "Attestation de déplacement")

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Constants.Url.CERTIFICATE_SHORTCUT_URI)
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

            builder
                .setIcon(Icon.createWithResource(this, R.drawable.ic_document))
                .setIntent(intent)
            builder.build()
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createUniversalQrCodeShortcut(): ShortcutInfo {
        val builder = ShortcutInfo.Builder(this, UNIVERSAL_QRCODE_SHORTCUT_ID)

        builder.setShortLabel(strings["appShortcut.qrScan"] ?: "Scanner QR Code TousAntiCovid")
        builder.setLongLabel(
            strings["universalQrScanController.explanation"]
                ?: "Scannez n'importe quel QR Code TousAntiCovid (cahier de rappel, certificats, déclaration Covid...)"
        )

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(Constants.Url.UNIVERSAL_QRCODE_SHORTCUT_URI)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        builder
            .setIcon(Icon.createWithResource(this, R.drawable.ic_qrscan))
            .setIntent(intent)
        return builder.build()
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

    companion object {
        private const val CURFEW_CERTIFICATE_SHORTCUT_ID: String = "curfewCertificateShortcut"
        private const val UNIVERSAL_QRCODE_SHORTCUT_ID: String = "universalQRCodeShortcut"
    }
}
