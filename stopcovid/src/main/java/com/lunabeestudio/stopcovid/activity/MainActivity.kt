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
import androidx.activity.viewModels
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
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.LocalizedApplication
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.databinding.ItemDividerBinding
import com.lunabeestudio.stopcovid.coreui.extension.applyAndConsumeWindowInsetBottom
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLanguage
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.extension.showSnackBar
import com.lunabeestudio.stopcovid.coreui.extension.userLanguage
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.databinding.ActivityMainBinding
import com.lunabeestudio.stopcovid.databinding.DialogUserLanguageBinding
import com.lunabeestudio.stopcovid.databinding.ItemSelectionBinding
import com.lunabeestudio.stopcovid.extension.alertRiskLevelChanged
import com.lunabeestudio.stopcovid.extension.flaggedCountry
import com.lunabeestudio.stopcovid.extension.googleReviewShown
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isLaunchedFromHistory
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.ratingPopInShown
import com.lunabeestudio.stopcovid.extension.ratingsKeyFiguresOpening
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.showAlertRiskLevelChanged
import com.lunabeestudio.stopcovid.extension.showRatingDialog
import com.lunabeestudio.stopcovid.extension.walletRepository
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.viewmodel.MainViewModel
import com.lunabeestudio.stopcovid.viewmodel.MainViewModelFactory
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.Locale

class MainActivity : BaseActivity() {

    lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            walletRepository(),
        )
    }

    private val navController: NavController by lazy {
        supportFragmentManager.findFragmentById(R.id.navHostFragment)!!.findNavController()
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val robertManager: RobertManager by lazy { robertManager() }

    private val strings: LocalizedStrings
        get() = (application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

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

        showLanguageDialogIfNeeded()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            setupAppShortcuts()
        }

        viewModel.walletCertificateLiveData.observe(this) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                setupAppShortcuts()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (sharedPrefs.alertRiskLevelChanged) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork(Constants.WorkerNames.AT_RISK_NOTIFICATION)

            MaterialAlertDialogBuilder(this).showAlertRiskLevelChanged(
                strings,
                sharedPrefs,
                injectionContainer.risksLevelManager.getCurrentLevel(robertManager().atRiskStatus?.riskLevel),
            )
        }
        showRatingDialogIfNeeded()
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
        } else {
            strings["universalQrScanController.error.wrongUrl"]?.let { errorMessage ->
                showErrorSnackBar(errorMessage, Snackbar.LENGTH_SHORT)
            }
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
        (application as? LocalizedApplication)?.liveLocalizedStrings?.observeEventAndConsume(this) {
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
        val noAppBarFragments = listOf(
            R.id.reportQrCodeFragment,
            R.id.venueQrCodeFragment,
            R.id.walletQRCodeFragment,
            R.id.universalQrScanFragment,
            R.id.verifyWalletQRCodeFragment,
            R.id.vaccineCompletionFragment,
            R.id.walletQuantityWarningFragment,
            R.id.importQrBottomSheetDialogFragment,
        )

        val forceLightStatusIconFragments = listOf(
            R.id.reportQrCodeFragment,
            R.id.venueQrCodeFragment,
            R.id.walletQRCodeFragment,
            R.id.universalQrScanFragment,
            R.id.walletQuantityWarningFragment,
            R.id.importQrBottomSheetDialogFragment,
        )

        val tabBehaviorFragments = mapOf(
            R.id.walletContainerFragment to TabBehavior.IGNORE, // let inner fragment decides
            R.id.walletPagerFragment to TabBehavior.SHOW,
            R.id.walletFullscreenPagerFragment to TabBehavior.SHOW,
            R.id.keyFiguresPagerFragment to TabBehavior.SHOW,
            R.id.generateActivityPassBottomSheetFragment to TabBehavior.SHOW,
        ) // default to TAB_BEHAVIOR.HIDE

        val isNoAppBarFragment = noAppBarFragments.contains(destination.id)
        val shouldForceLightStatusIcon = forceLightStatusIconFragments.contains(destination.id)
        val tabBehavior = tabBehaviorFragments[destination.id] ?: TabBehavior.HIDE

        lifecycleScope.launchWhenStarted {
            val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars =
                if (shouldForceLightStatusIcon) {
                    false
                } else {
                    !isNightMode()
                }
            if (isNoAppBarFragment) {
                // wait for default fragment switch animation time
                delay(200L)

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

                binding.appBarLayout.isVisible = true

                // Fix issue where appBarLayout take space even when gone
                val params = binding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
                params.behavior = AppBarLayout.ScrollingViewBehavior()
            }

            when (tabBehavior) {
                TabBehavior.SHOW -> binding.tabLayout.isVisible = true
                TabBehavior.HIDE -> binding.tabLayout.isVisible = false
                TabBehavior.IGNORE -> {
                    /* no-op */
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun setupAppShortcuts() {
        ContextCompat.getSystemService(this, ShortcutManager::class.java)?.let { shortcutManager ->
            val curfewCertificateShortcut = try {
                createCurfewCertificateShortcut()
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
            val universalQrCodeShortcut = createUniversalQrCodeShortcut()
            val favDccShortcut = createFavDccShortcut()

            shortcutManager.setDynamicShortcuts(listOfNotNull(curfewCertificateShortcut, universalQrCodeShortcut, favDccShortcut))
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
    private fun createFavDccShortcut(): ShortcutInfo? {
        val favCertificate = viewModel.walletCertificateLiveData.value?.firstOrNull {
            (it as? EuropeanCertificate)?.isFavorite == true
        } ?: return null

        val builder = ShortcutInfo.Builder(this, FAV_DCC_SHORTCUT_ID)

        builder.setShortLabel(strings["walletController.favoriteCertificateSection.title"] ?: "Mon certificat favori")
        builder.setLongLabel(strings["walletController.favoriteCertificateSection.title"] ?: "Mon certificat favori")

        val url: String = Constants.Url.DCC_FULLSCREEN_SHORTCUT_URI + favCertificate.id
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        builder
            .setIcon(Icon.createWithResource(this, R.drawable.ic_filled_heart))
            .setIntent(intent)
        return builder.build()
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

    fun showLanguageDialogIfNeeded() {
        if (sharedPrefs.userLanguage == null &&
            UiConstants.SUPPORTED_LOCALES.map { it.language }.none { it == Locale.getDefault().language }
        ) {

            val userLanguageSelectionView = DialogUserLanguageBinding.inflate(layoutInflater)

            var languageViewMap: Map<Locale, ItemSelectionBinding> = emptyMap()
            languageViewMap = UiConstants.SUPPORTED_LOCALES.associateWith { locale ->
                val selectionBinding = ItemSelectionBinding.inflate(layoutInflater, userLanguageSelectionView.root, false)
                selectionBinding.titleTextView.setTextOrHide(locale.flaggedCountry)
                selectionBinding.captionTextView.isVisible = false
                selectionBinding.selectionImageView.isInvisible = locale.language != getApplicationLanguage()
                selectionBinding.root.setOnClickListener {
                    sharedPrefs.userLanguage = locale.language
                    languageViewMap.forEach { (loopLocale, loopSelectionBinding) ->
                        loopSelectionBinding.selectionImageView.isInvisible = loopLocale.language != locale.language
                    }
                }
                selectionBinding
            }

            languageViewMap.values.forEachIndexed { idx, selectionBinding ->
                val divider = ItemDividerBinding.inflate(layoutInflater, userLanguageSelectionView.root, false)
                val dividerPos = idx * 2
                userLanguageSelectionView.root.addView(divider.root, dividerPos)
                userLanguageSelectionView.root.addView(selectionBinding.root, dividerPos + 1)
            }

            userLanguageSelectionView.userLanguageDialogFooter.text = strings["userLanguageController.footer"]

            MaterialAlertDialogBuilder(this)
                .setTitle(strings["userLanguageController.title"])
                .setMessage(strings["userLanguageController.subtitle"])
                .setView(userLanguageSelectionView.root)
                .setPositiveButton(strings["userLanguageController.button.title"]) { _, _ ->
                    if (sharedPrefs.userLanguage == null) {
                        sharedPrefs.userLanguage = getApplicationLanguage()
                    }
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun showRatingDialogIfNeeded() {
        val keyFiguresOpeningThreshold = robertManager.configuration.ratingsKeyFiguresOpeningThreshold.toLong()
        if (!sharedPrefs.googleReviewShown && sharedPrefs.ratingsKeyFiguresOpening >= keyFiguresOpeningThreshold) {
            if (!sharedPrefs.ratingPopInShown) {
                MaterialAlertDialogBuilder(this).showRatingDialog(strings) {
                    if (!ConfigConstant.Store.GOOGLE.openInExternalBrowser(this, false)) {
                        if (!ConfigConstant.Store.HUAWEI.openInExternalBrowser(this, false)) {
                            ConfigConstant.Store.TAC_WEBSITE.openInExternalBrowser(this)
                        }
                    }
                }
                sharedPrefs.ratingPopInShown = true
            }
        }
    }

    private enum class TabBehavior { SHOW, HIDE, IGNORE }

    companion object {
        private const val CURFEW_CERTIFICATE_SHORTCUT_ID: String = "curfewCertificateShortcut"
        private const val UNIVERSAL_QRCODE_SHORTCUT_ID: String = "universalQRCodeShortcut"
        private const val FAV_DCC_SHORTCUT_ID: String = "favDccShortcut"
    }
}
