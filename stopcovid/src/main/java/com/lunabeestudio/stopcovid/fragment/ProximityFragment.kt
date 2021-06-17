/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.LayoutDirection
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navOptions
import androidx.preference.PreferenceManager
import com.airbnb.lottie.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.addRipple
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.CardWithActionsItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.coreui.model.CardTheme
import com.lunabeestudio.stopcovid.extension.addIsolationItems
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.colorStringKey
import com.lunabeestudio.stopcovid.extension.formatNumberIfNeeded
import com.lunabeestudio.stopcovid.extension.getDepartmentLabel
import com.lunabeestudio.stopcovid.extension.getGradientBackground
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.getRelativeDateTimeString
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.hasChosenPostalCode
import com.lunabeestudio.stopcovid.extension.hideRiskStatus
import com.lunabeestudio.stopcovid.extension.isolationManager
import com.lunabeestudio.stopcovid.extension.labelShortStringKey
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showPostalCodeDialog
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.fastitem.LogoItem
import com.lunabeestudio.stopcovid.fastitem.NumbersCardItem
import com.lunabeestudio.stopcovid.fastitem.OnOffLottieItem
import com.lunabeestudio.stopcovid.fastitem.ProximityButtonItem
import com.lunabeestudio.stopcovid.fastitem.State
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.changePostalCodeItem
import com.lunabeestudio.stopcovid.fastitem.highlightedNumberCardItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.numbersCardItem
import com.lunabeestudio.stopcovid.fastitem.onOffLottieItem
import com.lunabeestudio.stopcovid.fastitem.proximityButtonItem
import com.lunabeestudio.stopcovid.manager.AppMaintenanceManager
import com.lunabeestudio.stopcovid.manager.InfoCenterManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.manager.RisksLevelManager
import com.lunabeestudio.stopcovid.manager.VaccinationCenterManager
import com.lunabeestudio.stopcovid.manager.VenuesManager
import com.lunabeestudio.stopcovid.model.CaptchaNextFragment
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.DeviceSetup
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.NoEphemeralBluetoothIdentifierFound
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModel
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class ProximityFragment : TimeMainFragment() {

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    internal val isolationManager by lazy {
        requireContext().isolationManager()
    }

    private val viewModel: ProximityViewModel by viewModels {
        ProximityViewModelFactory(robertManager, isolationManager, requireContext().secureKeystoreDataSource())
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private var onOffLottieItem: OnOffLottieItem? = null
    private var logoItem: LogoItem? = null
    private var proximityButtonItem: ProximityButtonItem? = null
    private lateinit var infoCenterCardItem: CardWithActionsItem
    private var healthItem: CardWithActionsItem? = null
    private var shouldRefresh: Boolean = false
    private var isProximityOn: Boolean = false
    private val interpolator = DecelerateInterpolator()
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            refreshItems(context?.let {
                ProximityManager.getDeviceSetup(it, robertManager)
            })
        }
    }
    private val errorReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val exception = intent.getSerializableExtra(Constants.Notification.SERVICE_ERROR_EXTRA) as? RobertException
            currentServiceError = exception.toCovidException()
            showErrorSnackBar(exception.toCovidException().getString(strings))
            refreshItems(context?.let {
                ProximityManager.getDeviceSetup(it, robertManager)
            })
        }
    }
    private var proximityClickThreshold = 0L
    private var lastAdapterRefresh: Long = 0L
    private var currentServiceError: CovidException? = null
    private var boundedService: ProximityService? = null

    private val proximityServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundedService = (service as ProximityService.ProximityBinder).getService()

            val handleError = { error: RobertException? ->
                context?.let {
                    currentServiceError = error?.toCovidException()
                    currentServiceError?.let {
                        showErrorSnackBar(it.getString(strings))
                    }
                    refreshScreen()
                } ?: Unit
            }

            boundedService?.getLastError()?.let(handleError)
            boundedService?.onError = handleError
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    override fun getTitleKey(): String {
        val deviceSetup = context?.let {
            ProximityManager.getDeviceSetup(it, robertManager)
        }
        return when {
            deviceSetup == DeviceSetup.NO_BLE -> "app.name"
            ProximityManager.isProximityOn(requireContext(), robertManager) && deviceSetup == DeviceSetup.BLE -> "home.title.activated"
            else -> "home.title.deactivated"
        }
    }

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Constants.SharedPrefs.HAS_NEWS) {
            refreshItems(context?.let {
                ProximityManager.getDeviceSetup(it, robertManager)
            })
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        activity?.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        activity?.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        activity?.registerReceiver(errorReceiver, IntentFilter(Constants.Notification.SERVICE_ERROR))

        getActivityBinding()?.errorLayout?.let { errorLayout ->
            errorLayout.post {
                errorLayout.translationY = errorLayout.height.toFloat()
            }
        }

        isProximityOn = ProximityManager.isProximityOn(requireContext(), robertManager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            setupAppShortcuts()
        }
        if (arguments?.getBoolean(START_PROXIMITY_ARG_KEY) == true) {
            arguments?.remove(START_PROXIMITY_ARG_KEY)
            activateProximity()
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun setupAppShortcuts() {
        getSystemService(requireContext(), ShortcutManager::class.java)?.let { shortcutManager ->
            val certificateShortcut = createCertificateShortcut()
            val venueQrCodeShortcut = createVenueQrCodeShortcut()

            shortcutManager.setDynamicShortcuts(listOfNotNull(certificateShortcut, venueQrCodeShortcut))
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createCertificateShortcut(): ShortcutInfo? {
        return if (robertManager.configuration.displayAttestation) {
            val builder = ShortcutInfo.Builder(context, CURFEW_CERTIFICATE_SHORTCUT_ID)

            builder.setShortLabel(strings["attestationsController.title"] ?: "Attestations")
            builder.setLongLabel(strings["home.moreSection.curfewCertificate"] ?: "Attestation de déplacement")

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(CURFEW_CERTIFICATE_SHORTCUT_URI)
            )
            builder
                .setIcon(Icon.createWithResource(context, R.drawable.ic_document))
                .setIntent(intent)
            builder.build()
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createVenueQrCodeShortcut(): ShortcutInfo? {
        return if (robertManager.configuration.displayRecordVenues) {
            val builder = ShortcutInfo.Builder(context, VENUE_QRCODE_SHORTCUT_ID)

            builder.setShortLabel(strings["appShortcut.venues"] ?: "Scanner un lieu")
            builder.setLongLabel(strings["home.venuesSection.recordCell.title"] ?: "Enregistrez l’historique des lieux que vous fréquentez")

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(VENUE_QRCODE_SHORTCUT_URI)
            )
            builder
                .setIcon(Icon.createWithResource(context, R.drawable.ic_qr_code_scanner))
                .setIntent(intent)
            builder.build()
        } else {
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
        initHasNewsObserver()

        shouldRefresh = false

        findNavControllerOrNull()?.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>(PostalCodeBottomSheetFragment.SHOULD_BE_REFRESHED_KEY)?.observe(
                viewLifecycleOwner
            ) { shouldBeRefreshed ->
                if (shouldBeRefreshed) {
                    refreshScreen()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        bindToProximityService()
    }

    override fun onResume() {
        super.onResume()
        if (shouldRefresh) {
            refreshScreen()
            shouldRefresh = false
        }
    }

    private fun initViewModelObserver() {
        viewModel.loadingInProgress.observe(viewLifecycleOwner) { loadingInProgress ->
            (activity as? MainActivity)?.showProgress(loadingInProgress)
        }
        viewModel.covidException.observe(viewLifecycleOwner) { covidException ->
            covidException?.let {
                if (it is NoEphemeralBluetoothIdentifierFound) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(strings["error.cryptoIssue.explanation.title"])
                        .setMessage(strings["error.cryptoIssue.explanation.message"])
                        .setPositiveButton(strings["error.cryptoIssue.explanation.register"]) { _, _ ->
                            viewModel.clearData(requireContext().applicationContext as RobertApplication)
                        }
                        .setNeutralButton(strings["error.cryptoIssue.explanation.goToStore"]) { _, _ ->
                            context?.let { context ->
                                "${Constants.Url.PLAY_STORE_URL}${context.packageName}".openInExternalBrowser(context)
                            }
                        }
                        .setNegativeButton(strings["common.cancel"], null)
                        .show()
                } else {
                    showErrorSnackBar(it.getString(strings))
                }
            }
            refreshItems(context?.let {
                ProximityManager.getDeviceSetup(it, robertManager)
            })
        }
        viewModel.clearDataSuccess.observe(viewLifecycleOwner) {
            activateProximity()
        }
        viewModel.activateProximitySuccess.observe(viewLifecycleOwner) {
            refreshItems(context?.let {
                ProximityManager.getDeviceSetup(it, robertManager)
            })
            (requireContext().applicationContext as StopCovid).cancelActivateReminder()
        }
        viewModel.isolationFormState.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.isolationDataChanged.observe(viewLifecycleOwner) {
            refreshScreen()
        }
        robertManager.liveAtRiskStatus.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        robertManager.liveUpdatingRiskStatus.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        InfoCenterManager.infos.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        InfoCenterManager.strings.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        KeyFiguresManager.figures.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.activeAttestationCount.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    private fun initHasNewsObserver() {
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    private fun bindToProximityService() {
        requireActivity().bindService(ProximityService.intent(requireActivity()), proximityServiceConnection, Context.BIND_ABOVE_CLIENT)
    }

    override fun onPause() {
        super.onPause()
        shouldRefresh = true
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unbindService(proximityServiceConnection)
        boundedService?.onError = null
        boundedService = null
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        val isSick = robertManager.isSick
        val deviceSetup = context?.let {
            ProximityManager.getDeviceSetup(it, robertManager)
        }

        if (deviceSetup != DeviceSetup.NO_BLE) {
            addTopImageItems(items)
        }
        if (deviceSetup != DeviceSetup.NO_BLE) {
            addActivateButtonItems(items, deviceSetup)
        }

        if (AppMaintenanceManager.shouldDisplayUpdateAvailable) {
            if (deviceSetup == DeviceSetup.NO_BLE) {
                items += spaceItem {
                    spaceRes = R.dimen.spacing_medium
                    identifier = items.count().toLong()
                }
            }
            addAppUpdateItems(items)
        }
        addSectionSeparator(items)

        // Health items
        addHealthItems(items, isSick)
        if (robertManager.configuration.displayIsolation) {
            addIsolationItems(items)
        }
        if ((deviceSetup != DeviceSetup.NO_BLE || robertManager.configuration.displayRecordVenues) && !isSick) {
            addDeclareItems(items)
        }
        if (robertManager.configuration.displayVaccination) {
            addVaccinationItems(items)
        }
        addSectionSeparator(items)

        // News items
        addNewsItems(items)
        addSectionSeparator(items)

        // Attestation
        if (robertManager.configuration.displayAttestation) {
            addAttestationItems(items)
            addSectionSeparator(items)
        }

        // Venue items
        if (robertManager.configuration.displayRecordVenues) {
            addVenueItems(items)
            addSectionSeparator(items)
        }

        // More items
        addMoreItems(items)
        addSectionSeparator(items)

        refreshItems(deviceSetup)

        return items
    }

    private fun addSectionSeparator(items: ArrayList<GenericItem>) {
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
    }

    private fun addAppUpdateItems(items: ArrayList<GenericItem>) {
        items += cardWithActionItem {
            mainTitle = strings["home.appUpdate.cell.title"]
            mainBody = strings["home.appUpdate.cell.subtitle"]
            identifier = items.count().toLong()
            mainImage = R.drawable.app_update_card
            mainLayoutDirection = LayoutDirection.RTL
            onCardClick = {
                if (!ConfigConstant.Store.GOOGLE.openInExternalBrowser(requireContext(), false)) {
                    if (!ConfigConstant.Store.HUAWEI.openInExternalBrowser(requireContext(), false)) {
                        ConfigConstant.Store.WEBSITE.openInExternalBrowser(requireContext())
                    }
                }
            }
        }
    }

    private fun addTopImageItems(items: ArrayList<GenericItem>) {
        onOffLottieItem = onOffLottieItem {
            identifier = items.count().toLong()
        }
        logoItem = logoItem {
            identifier = items.count().toLong()
        }
        if (isAnimationEnabled()) {
            onOffLottieItem?.let { items += it }
        } else {
            logoItem?.let { items += it }
        }
    }

    private fun addActivateButtonItems(items: ArrayList<GenericItem>, deviceSetup: DeviceSetup?) {
        proximityButtonItem = proximityButtonItem {
            mainText = strings["home.mainButton.activate"]
            lightText = strings["home.mainButton.deactivate"]
            onClickListener = View.OnClickListener {
                onProximityButtonClick(deviceSetup)
            }
            identifier = items.count().toLong()
        }

        when {
            robertManager.isRegistered -> {
                proximityButtonItem?.let { items += it }
            }
            !ProximityManager.isAdvertisingValid(robertManager) -> {
                items += cardWithActionItem(CardTheme.Primary) {
                    mainBody = strings["proximityController.error.noAdvertising"]
                    identifier = items.count().toLong()
                }
            }
            else -> {
                items += cardWithActionItem(CardTheme.Primary) {
                    mainBody = strings["home.activationExplanation"]
                    onCardClick = {
                        onProximityButtonClick(deviceSetup)
                    }
                    identifier = items.count().toLong()
                    actions = listOf(
                        Action(label = strings["home.mainButton.activate"]) {
                            onProximityButtonClick(deviceSetup)
                        }
                    )
                }
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun onProximityButtonClick(deviceSetup: DeviceSetup?) {
        if (SystemClock.elapsedRealtime() > proximityClickThreshold) {
            proximityClickThreshold = SystemClock.elapsedRealtime() + PROXIMITY_BUTTON_DELAY
            when {
                robertManager.isProximityActive -> {
                    deactivateProximity()
                    refreshItems(deviceSetup)
                }
                ProximityManager.hasUnstableBluetooth() -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(strings["home.activationExplanation.title"])
                        .setMessage(strings["home.activationExplanation.message"])
                        .setPositiveButton(strings["common.ok"]) { _, _ ->
                            activateProximity()
                        }
                        .setNegativeButton(strings["common.cancel"], null)
                        .show()
                }
                else -> {
                    activateProximity()
                }
            }
        }
    }

    private fun addHealthItems(items: ArrayList<GenericItem>, isSick: Boolean) {
        items += bigTitleItem {
            text = strings["home.healthSection.title"]
            identifier = "home.healthSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        when {
            isSick -> {
                healthItem = null
                items += cardWithActionItem(CardTheme.Sick) {
                    mainImage = R.drawable.health_card
                    mainTitle = strings["home.healthSection.isSick.standaloneTitle"]
                    onCardClick = {
                        AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e5, null)
                        findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToIsSickFragment())
                    }
                    identifier = R.drawable.health_card.toLong()
                }
            }
            !sharedPrefs.hideRiskStatus -> {
                healthItem = RisksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.let {
                    cardWithActionItem(CardTheme.Color) {
                        mainImage = R.drawable.health_card
                        gradientBackground = it.getGradientBackground()
                        onCardClick = {
                            AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e5, null)
                            findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToHealthFragment())
                        }
                        identifier = R.drawable.health_card.toLong()

                        actions = refreshStatusActions(robertManager.liveUpdatingRiskStatus.value?.peekContent())?.let { listOf(it) }
                    }
                }

                healthItem?.let { healthItem ->
                    items += healthItem
                }
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun addVenueItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.venuesSection.title"]
            identifier = "home.venuesSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += cardWithActionItem(CardTheme.Primary) {
            mainImage = R.drawable.signal_card
            onCardClick = {
                startRecordVenue()
            }
            mainTitle = strings["home.venuesSection.recordCell.title"]
            mainBody = strings["home.venuesSection.recordCell.subtitle"]
            identifier = R.drawable.signal_card.toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun startRecordVenue() {
        AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e12, null)
        findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToVenueQrCodeFragment())
    }

    private fun addNewsItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.infoSection.title"]
            identifier = items.count().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += getKeyFiguresItems()

        infoCenterCardItem = cardWithActionItem {
            cardTitle = strings["home.infoSection.lastInfo"]
            cardTitleIcon = R.drawable.ic_bell

            contentDescription = strings["home.infoSection.readAll"]

            identifier = cardTitle.hashCode().toLong()
            mainMaxLines = 3
            onCardClick = {
                AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e10, null)
                findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToInfoCenterFragment())
            }

            actions = listOf(
                Action(
                    label = strings["home.infoSection.readAll"],
                    showBadge = sharedPrefs.getBoolean(Constants.SharedPrefs.HAS_NEWS, false)
                ) {
                    AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e10, null)
                    findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToInfoCenterFragment())
                }
            )
        }
        items += infoCenterCardItem

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun getKeyFiguresItems(): ArrayList<GenericItem> {
        val items = ArrayList<GenericItem>()
        var isHighlighted = false
        val darkMode = requireContext().isNightMode()

        val keyFiguresClickListener = View.OnClickListener {
            AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e8, null)
            findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToKeyFiguresFragment())
        }

        KeyFiguresManager.highlightedFigures?.let { figure ->
            items += highlightedNumberCardItem {
                label = "${strings[figure.labelShortStringKey]} (${strings["common.country.france"]})"
                updatedAt = strings["keyfigure.dailyUpdates"]
                value = figure.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
                onClickListener = View.OnClickListener {
                    AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e9, null)
                    findNavControllerOrNull()?.safeNavigate(
                        ProximityFragmentDirections.actionProximityFragmentToKeyFigureDetailsFragment(
                            figure.labelKey
                        )
                    )
                }
                strings[figure.colorStringKey(darkMode)]?.let {
                    color = Color.parseColor(it)
                }
                identifier = "highlightedNumberCard".hashCode().toLong()
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_medium
                identifier = items.count().toLong()
            }
            isHighlighted = true
        }

        items += numbersCardItem {
            if (isHighlighted) {
                subheader = null
                header = strings["home.infoSection.otherKeyFigures"]
            } else {
                subheader = strings["keyfigure.dailyUpdates"]
                header = strings["home.infoSection.keyFigures"]
            }
            link = strings["home.infoSection.seeAll"]
            contentDescription = strings["home.infoSection.seeAll"]
            onClickListener = keyFiguresClickListener
            identifier = header.hashCode().toLong()

            KeyFiguresManager.featuredFigures?.let { keyFigures ->
                franceData = NumbersCardItem.Data(
                    strings["common.country.france"],
                    generateFromKeyFigure(keyFigures.getOrNull(0)),
                    generateFromKeyFigure(keyFigures.getOrNull(1)),
                    generateFromKeyFigure(keyFigures.getOrNull(2))
                )

                localData = if (sharedPrefs.hasChosenPostalCode) {
                    NumbersCardItem.Data(
                        keyFigures.getDepartmentLabel(sharedPrefs.chosenPostalCode),
                        generateFromKeyFigure(keyFigures.getOrNull(0), true),
                        generateFromKeyFigure(keyFigures.getOrNull(1), true),
                        generateFromKeyFigure(keyFigures.getOrNull(2), true)
                    )
                } else {
                    null
                }
            }
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }

        if (robertManager.configuration.displayDepartmentLevel) {
            if (sharedPrefs.chosenPostalCode == null) {
                items += cardWithActionItem(CardTheme.Primary) {
                    cardTitle = strings["home.infoSection.newPostalCode"]
                    cardTitleIcon = R.drawable.ic_map
                    mainBody = strings["home.infoSection.newPostalCode.subtitle"]
                    onCardClick = {
                        showPostalCodeDialog()
                    }
                    actions = listOf(
                        Action(label = strings["home.infoSection.newPostalCode.button"]) {
                            showPostalCodeDialog()
                        }
                    )
                    contentDescription = strings["home.infoSection.newPostalCode.subtitle"]
                    identifier = "home.infoSection.newPostalCode".hashCode().toLong()
                }
            } else {
                items += changePostalCodeItem {
                    label = stringsFormat("common.updatePostalCode", sharedPrefs.chosenPostalCode)
                    endLabel = strings["common.updatePostalCode.end"]
                    iconRes = R.drawable.ic_map
                    onClickListener = View.OnClickListener {
                        findNavControllerOrNull()
                            ?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToPostalCodeBottomSheetFragment())
                    }
                    identifier = "common.updatePostalCode".hashCode().toLong()
                }
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_medium
                identifier = items.count().toLong()
            }
        }

        return items
    }

    private fun generateFromKeyFigure(keyFigure: KeyFigure?, fromDepartment: Boolean = false): NumbersCardItem.DataFigure? {
        return keyFigure?.let {
            NumbersCardItem.DataFigure(
                strings[keyFigure.labelShortStringKey],
                if (fromDepartment) {
                    keyFigure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)?.valueToDisplay?.formatNumberIfNeeded(numberFormat)
                } else {
                    keyFigure.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
                },
                strings[keyFigure.colorStringKey(requireContext().isNightMode())]?.let {
                    Color.parseColor(it)
                }
            )
        }
    }

    private fun addAttestationItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.attestationSection.title"]
            identifier = items.count().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += cardWithActionItem {
            mainImage = R.drawable.attestation_card
            onCardClick = {
                AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e11, null)
                findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToAttestationsFragment())
            }
            mainTitle = strings["home.attestationSection.cell.title"]
            val attestationCount = viewModel.activeAttestationCount.value
            mainBody = when (val count = attestationCount?.peekContent()) {
                0, null -> strings["home.attestationSection.cell.subtitle.noAttestations"]
                1 -> strings["home.attestationSection.cell.subtitle.oneAttestation"]
                else -> stringsFormat("home.attestationSection.cell.subtitle.multipleAttestations", count)
            }
            identifier = R.drawable.attestation_card.toLong()
        }

        if (robertManager.configuration.displaySanitaryCertificatesWallet) {
            items += spaceItem {
                spaceRes = R.dimen.spacing_medium
                identifier = items.count().toLong()
            }

            items += cardWithActionItem(CardTheme.Primary) {
                mainImage = R.drawable.wallet_card
                mainLayoutDirection = LayoutDirection.RTL
                onCardClick = {
                    findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToWalletContainerFragment())
                }
                mainTitle = strings["home.attestationSection.sanitaryCertificates.cell.title"]
                mainBody = strings["home.attestationSection.sanitaryCertificates.cell.subtitle"]
                identifier = R.drawable.wallet_card.toLong()
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun showPostalCodeDialog() {
        MaterialAlertDialogBuilder(requireContext()).showPostalCodeDialog(
            layoutInflater,
            strings
        ) { postalCode ->
            sharedPrefs.chosenPostalCode = postalCode
            viewLifecycleOwner.lifecycleScope.launch {
                (activity as? MainActivity)?.showProgress(true)
                VaccinationCenterManager.postalCodeDidUpdate(requireContext(), sharedPrefs, postalCode)
                (activity as? MainActivity)?.showProgress(false)
                refreshScreen()
            }
        }
    }

    private fun addDeclareItems(items: ArrayList<GenericItem>) {
        items += cardWithActionItem {
            mainTitle = strings["home.declareSection.cellTitle"]
            mainBody = strings["home.declareSection.cellSubtitle"]
            mainImage = R.drawable.declare_card
            contentDescription = strings["home.declareSection.title"]
            onCardClick = {
                findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToReportFragment())
            }
            identifier = mainTitle.hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun addVaccinationItems(items: ArrayList<GenericItem>) {
        items += cardWithActionItem {
            cardTitle = strings["home.vaccinationSection.cellTitle"]
            cardTitleColorInt = R.color.color_no_risk
            cardTitleIcon = R.drawable.ic_vaccin
            mainHeader = strings["home.vaccinationSection.cellSubtitle"]
            contentDescription = strings["home.vaccinationSection.cellTitle"]
            onCardClick = {
                AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e7, null)
                findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToVaccinationFragment())
            }
            identifier = "home.vaccinationSection.cellTitle".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun addMoreItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.moreSection.title"]
            identifier = "home.moreSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += cardWithActionItem {
            actions = listOfNotNull(
                Action(R.drawable.ic_link, strings["home.moreSection.usefulLinks"]) {
                    findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToLinksFragment())
                },
                Action(R.drawable.ic_share, strings["home.moreSection.appSharing"]) {
                    AnalyticsManager.reportAppEvent(requireContext(), AppEventName.e4, null)
                    ShareCompat.IntentBuilder.from(requireActivity())
                        .setType("text/plain")
                        .setText(strings["sharingController.appSharingMessage"])
                        .startChooser()
                },
                Action(R.drawable.ic_history, strings["home.moreSection.venuesHistory"]) {
                    findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToVenuesHistoryFragment())
                }.takeIf {
                    robertManager.configuration.displayRecordVenues || !VenuesManager.getVenuesQrCode(
                        requireContext().secureKeystoreDataSource(),
                    ).isNullOrEmpty()
                },
                Action(R.drawable.ic_settings, strings["common.settings"]) {
                    findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToManageDataFragment())
                },
                Action(R.drawable.ic_2d_doc, strings["home.moreSection.verifySanitaryCertificate"]) {
                    findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToVerifyWalletQRCodeFragment())
                }.takeIf {
                    robertManager.configuration.displaySanitaryCertificatesValidation
                },
                Action(R.drawable.ic_privacy, strings["home.moreSection.privacy"]) {
                    findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToPrivacyFragment())
                },
                Action(R.drawable.ic_about, strings["home.moreSection.aboutStopCovid"]) {
                    findNavControllerOrNull()?.safeNavigate(R.id.nav_about, null, navOptions {
                        anim {
                            enter = R.anim.nav_default_enter_anim
                            popEnter = R.anim.nav_default_pop_enter_anim
                            popExit = R.anim.nav_default_pop_exit_anim
                            exit = R.anim.nav_default_exit_anim
                        }
                    })
                }
            )
            identifier = "home.moreSection.title.content".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun activateProximity() {
        currentServiceError = null
        when {
            !robertManager.canActivateProximity -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["home.activation.sick.alert.title"])
                    .setMessage(strings["home.activation.sick.alert.message"])
                    .setPositiveButton(strings["common.ok"], null)
                    .show()
            }
            !robertManager.isRegistered -> {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    if (viewModel.refreshConfig(requireContext().applicationContext as RobertApplication)) {
                        withContext(Dispatchers.Main) {
                            findNavControllerOrNull()
                                ?.safeNavigate(
                                    ProximityFragmentDirections.actionProximityFragmentToCaptchaFragment(
                                        CaptchaNextFragment.Back,
                                        null
                                    )
                                )
                        }
                    }
                }
            }
            else -> {
                bindToProximityService()
                viewModel.activateProximity(requireContext().applicationContext as RobertApplication)
            }
        }
    }

    private fun deactivateProximity(showReminder: Boolean = true) {
        currentServiceError = null
        robertManager.deactivateProximity(requireContext().applicationContext as RobertApplication)
        view?.rootView?.announceForAccessibility(strings["notification.proximityServiceNotRunning.title"])
        if (showReminder) {
            findNavControllerOrNull()?.safeNavigate(ProximityFragmentDirections.actionProximityFragmentToReminderDialogFragment())
        }
    }

    @OptIn(ExperimentalTime::class)
    @SuppressLint("RestrictedApi")
    private fun refreshItems(deviceSetup: DeviceSetup?) {
        if (strings.isEmpty()) {
            return // Do nothing until strings are loaded
        }

        refreshHealthItem(requireContext())

        context?.let { context ->
            val freshProximityOn = ProximityManager.isProximityOn(context, robertManager)
            val wasProximityDifferent: Boolean = isProximityOn != freshProximityOn
            isProximityOn = freshProximityOn
            refreshTopImage(wasProximityDifferent)

            val infoCenterStrings = InfoCenterManager.strings.value?.peekContent() ?: emptyMap()

            InfoCenterManager.infos.value?.peekContent()?.firstOrNull()?.let { info ->
                if (::infoCenterCardItem.isInitialized) {
                    infoCenterCardItem.apply {
                        mainHeader = info.timestamp.seconds.getRelativeDateTimeString(requireContext(), strings["common.justNow"])
                        mainTitle = infoCenterStrings[info.titleKey]
                        mainBody = infoCenterStrings[info.descriptionKey]
                    }
                }
            }

            proximityButtonItem?.showMainButton = !isProximityOn
            proximityButtonItem?.isButtonEnabled = deviceSetup == DeviceSetup.BLE

            if (isAdded && !isHidden) {
                refreshTitleAndErrorLayout()
            }

            if (binding?.recyclerView?.isComputingLayout == false) {
                lastAdapterRefresh = System.currentTimeMillis()
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshTitleAndErrorLayout()
        }
    }

    private fun refreshTitleAndErrorLayout() {
        context?.let { context ->
            appCompatActivity?.supportActionBar?.title = strings[getTitleKey()]
            updateErrorLayout(getActivityBinding()?.errorLayout, ProximityManager.getDeviceSetup(context, robertManager))
        }
    }

    private fun refreshTopImage(wasProximityDifferent: Boolean) {
        onOffLottieItem?.state = if (wasProximityDifferent) {
            if (isProximityOn) {
                State.OFF_TO_ON
            } else {
                State.ON_TO_OFF
            }
        } else {
            if (isProximityOn) {
                State.ON
            } else {
                State.OFF
            }
        }
        logoItem?.imageRes = if (isProximityOn) {
            R.drawable.status_active
        } else {
            R.drawable.status_inactive
        }
    }

    override fun setTitle() {
        // Handled in refresh item
    }

    @OptIn(ExperimentalTime::class)
    private fun refreshHealthItem(context: Context) {
        healthItem?.apply {
            RisksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.let {
                mainTitle = strings[it.labels.homeTitle]
                mainBody = strings[it.labels.homeSub]

                mainHeader = getStatusLastUpdateToDisplay(context, robertManager.atRiskLastRefresh, it.riskLevel)
            }

            actions = refreshStatusActions(robertManager.liveUpdatingRiskStatus.value?.peekContent())?.let { listOf(it) }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun timeRefresh() {
        val notifyCalledMoreThanHalfAMinuteAgo = System.currentTimeMillis() - lastAdapterRefresh > TimeUnit.SECONDS.toMillis(30)
        if (notifyCalledMoreThanHalfAMinuteAgo) {
            viewLifecycleOwnerOrNull()?.lifecycleScope?.launch(Dispatchers.Default) {
                healthItem?.apply {
                    mainHeader = getStatusLastUpdateToDisplay(
                        requireContext(),
                        robertManager.atRiskLastRefresh,
                        robertManager.atRiskStatus?.riskLevel ?: 0f
                    )
                }
                InfoCenterManager.infos.value?.peekContent()?.firstOrNull()?.let { info ->
                    if (::infoCenterCardItem.isInitialized) {
                        infoCenterCardItem.apply {
                            mainHeader = info.timestamp.seconds.getRelativeDateTimeString(requireContext(), strings["common.justNow"])
                        }
                    }
                }

                if (binding?.recyclerView?.isComputingLayout == false) {
                    withContext(Dispatchers.Main) {
                        lastAdapterRefresh = System.currentTimeMillis()
                        binding?.recyclerView?.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun isAnimationEnabled(): Boolean = Utils.getAnimationScale(context) != 0f

    private fun updateErrorLayout(errorLayout: FrameLayout?, deviceSetup: DeviceSetup?) {
        getActivityBinding()?.errorTextView?.text = ProximityManager.getErrorText(this, robertManager, currentServiceError, strings)
        val clickListener = ProximityManager.getErrorClickListener(this, robertManager, currentServiceError, {
            if (SystemClock.elapsedRealtime() > proximityClickThreshold) {
                proximityClickThreshold = SystemClock.elapsedRealtime() + PROXIMITY_BUTTON_DELAY
                activateProximity()
            }
        }) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                deactivateProximity(false)
                delay(PROXIMITY_BUTTON_DELAY)
                activateProximity()

            }
        }
        getActivityBinding()?.errorTextView?.setOnClickListener(clickListener)
        if (clickListener != null) {
            getActivityBinding()?.errorTextView?.addRipple()
        } else {
            getActivityBinding()?.errorTextView?.background = null
        }
        if (getActivityBinding()?.errorTextView?.text.isNullOrEmpty()) {
            hideErrorLayout(errorLayout)
        } else if (deviceSetup != DeviceSetup.NO_BLE) {
            showErrorLayout(errorLayout)
        }
    }

    private fun hideErrorLayout(errorLayout: FrameLayout?) {
        errorLayout?.errorTextView?.isClickable = false
        errorLayout?.animate()?.apply {
            duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
            interpolator = this@ProximityFragment.interpolator
            setUpdateListener { valueAnimator ->
                binding?.recyclerView
                    ?.updatePadding(bottom = (errorLayout.height.toFloat() * (1f - valueAnimator.animatedValue as Float)).toInt())
            }
            translationY(errorLayout.height.toFloat())
            withEndAction {
                errorLayout.isInvisible = true
                binding?.recyclerView?.let {
                    getAppBarLayout()?.refreshLift(it)
                }
            }
            start()
        }
    }

    private fun showErrorLayout(errorLayout: FrameLayout?) {
        errorLayout?.post {
            context?.let {
                errorLayout.isVisible = true
                errorLayout.animate().apply {
                    duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                    interpolator = this@ProximityFragment.interpolator
                    setUpdateListener { valueAnimator ->
                        binding?.recyclerView
                            ?.updatePadding(bottom = (errorLayout.height.toFloat() * valueAnimator.animatedValue as Float).toInt())
                    }
                    translationY(0f)
                    withEndAction {
                        errorLayout.errorTextView.isClickable = true
                    }
                    start()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.Activity.BATTERY.ordinal) {
            if (resultCode == Activity.RESULT_OK) {
                refreshScreen()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroyView() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        activity?.unregisterReceiver(receiver)
        activity?.unregisterReceiver(errorReceiver)
        super.onDestroyView()
    }

    companion object {
        private const val PROXIMITY_BUTTON_DELAY: Long = 2000L
        private const val CURFEW_CERTIFICATE_SHORTCUT_ID: String = "curfewCertificateShortcut"
        private const val CURFEW_CERTIFICATE_SHORTCUT_URI: String = "tousanticovid://attestations/"
        private const val VENUE_QRCODE_SHORTCUT_ID: String = "venueQRCodeShortcut"
        const val VENUE_QRCODE_SHORTCUT_URI: String = "tousanticovid://venueQRCode/"
        const val START_PROXIMITY_ARG_KEY: String = "START_PROXIMITY_ARG_KEY"
    }
}
