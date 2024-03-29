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
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.LayoutDirection
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.webkit.URLUtil
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ShareCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navOptions
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.extension.addRipple
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLocale
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.openAppSettings
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.extension.showPermissionSettingsDialog
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.CardWithActionsItem
import com.lunabeestudio.stopcovid.coreui.fastitem.SpaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.horizontalRecyclerViewItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.coreui.model.CardTheme
import com.lunabeestudio.stopcovid.databinding.ActivityMainBinding
import com.lunabeestudio.stopcovid.databinding.FragmentRecyclerViewFabBinding
import com.lunabeestudio.stopcovid.extension.addIsolationItems
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.collectDataWithLifecycle
import com.lunabeestudio.stopcovid.extension.colorStringKey
import com.lunabeestudio.stopcovid.extension.formatNumberIfNeeded
import com.lunabeestudio.stopcovid.extension.generateCombinedData
import com.lunabeestudio.stopcovid.extension.getDefaultFigureLabel1
import com.lunabeestudio.stopcovid.extension.getDefaultFigureLabel2
import com.lunabeestudio.stopcovid.extension.getGradientBackground
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.getRelativeDateTimeString
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.hasChosenPostalCode
import com.lunabeestudio.stopcovid.extension.hideRiskStatus
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isLowStorage
import com.lunabeestudio.stopcovid.extension.isValidUUID
import com.lunabeestudio.stopcovid.extension.isolationManager
import com.lunabeestudio.stopcovid.extension.keyFigureCompare1
import com.lunabeestudio.stopcovid.extension.keyFigureCompare2
import com.lunabeestudio.stopcovid.extension.labelShortStringKey
import com.lunabeestudio.stopcovid.extension.lowStorageAlertShown
import com.lunabeestudio.stopcovid.extension.notificationVersionClosed
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showErrorPanel
import com.lunabeestudio.stopcovid.extension.showAlertSickVenue
import com.lunabeestudio.stopcovid.extension.showErrorSnackBar
import com.lunabeestudio.stopcovid.extension.showPostalCodeDialog
import com.lunabeestudio.stopcovid.extension.showActivationReminderDialog
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.fastitem.CompareFigureChartItem
import com.lunabeestudio.stopcovid.fastitem.LogoItem
import com.lunabeestudio.stopcovid.fastitem.OnOffLottieItem
import com.lunabeestudio.stopcovid.fastitem.ProximityButtonItem
import com.lunabeestudio.stopcovid.fastitem.State
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.coreui.fastitem.TitleItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.changePostalCodeItem
import com.lunabeestudio.stopcovid.fastitem.compareFigureCardChartItem
import com.lunabeestudio.stopcovid.fastitem.explanationActionCardItem
import com.lunabeestudio.stopcovid.fastitem.homeScreeFigureCardItem
import com.lunabeestudio.stopcovid.fastitem.homeScreeInfoCardItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.onOffLottieItem
import com.lunabeestudio.stopcovid.fastitem.proximityButtonItem
import com.lunabeestudio.stopcovid.fastitem.smallQrCodeCardItem
import com.lunabeestudio.stopcovid.manager.AppMaintenanceManager
import com.lunabeestudio.stopcovid.manager.ChartManager
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.CaptchaNextFragment
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.DeeplinkOrigin
import com.lunabeestudio.stopcovid.model.DeviceSetup
import com.lunabeestudio.stopcovid.model.KeyFiguresNotAvailableException
import com.lunabeestudio.stopcovid.model.NoEphemeralBluetoothIdentifierFound
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.utils.ExtendedFloatingActionButtonScrollListener
import com.lunabeestudio.stopcovid.utils.lazyFast
import com.lunabeestudio.stopcovid.viewmodel.HomeViewModel
import com.lunabeestudio.stopcovid.viewmodel.HomeViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class HomeFragment : TimeMainFragment() {

    override val layout: Int = R.layout.fragment_recycler_view_fab

    private var fragmentRecyclerViewFabBinding: FragmentRecyclerViewFabBinding? = null

    private val numberFormat: NumberFormat by lazyFast { NumberFormat.getNumberInstance(context.getApplicationLocale()) }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    internal val isolationManager by lazy {
        requireContext().isolationManager()
    }

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            robertManager,
            isolationManager,
            requireContext().secureKeystoreDataSource(),
            vaccinationCenterManager,
            venueRepository,
            walletRepository,
            sharedPrefs,
            injectionContainer.getSmartWalletMapUseCase,
            injectionContainer.getSmartWalletStateUseCase,
        )
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val barcodeEncoder by lazyFast { BarcodeEncoder() }

    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    private var permissionResultLauncher: ActivityResultLauncher<Array<String>>? = null

    private var onOffLottieItem: OnOffLottieItem? = null
    private var logoItem: LogoItem? = null
    private var proximityButtonItem: ProximityButtonItem? = null
    private lateinit var subTitleItem: TitleItem
    private var healthItem: CardWithActionsItem? = null
    private var shouldRefresh: Boolean = false
    private var isProximityOn: Boolean = false
    private val interpolator = DecelerateInterpolator()
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            refreshItems(
                context?.let {
                    ProximityManager.getDeviceSetup(it, robertManager)
                }
            )
        }
    }
    private val errorReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val exception = intent.getSerializableExtra(Constants.Notification.SERVICE_ERROR_EXTRA) as? RobertException
            currentServiceError = exception.toCovidException()
            showErrorSnackBar(exception.toCovidException().getString(strings))
            refreshItems(
                context?.let {
                    ProximityManager.getDeviceSetup(it, robertManager)
                }
            )
        }
    }
    private var proximityClickThreshold = 0L
    private var lastAdapterRefresh: Long = 0L
    private var currentServiceError: CovidException? = null
    private var boundedService: ProximityService? = null
    private var showErrorLayoutAnimationInProgress: Boolean = false
    private var hideErrorLayoutAnimationInProgress: Boolean = false
    private var hasShownRegisterAlert: Boolean = false
    private val recyclerViewPool = RecyclerView.RecycledViewPool()

    private val proximityServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundedService = (service as ProximityService.ProximityBinder).getService()

            val handleError = { error: RobertException? ->
                context?.let {
                    viewLifecycleOwnerOrNull()?.lifecycleScope?.launch(Dispatchers.Main) {
                        currentServiceError = error?.toCovidException()
                        currentServiceError?.let {
                            showErrorSnackBar(it.getString(strings))
                        }
                        refreshScreen()
                    }
                }
            }

            boundedService?.getLastError()?.let(handleError)
            boundedService?.onError = handleError
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    override fun getTitleKey(): String {
        //return "proximityController.tabBar.title"
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
            refreshItems(
                context?.let {
                    ProximityManager.getDeviceSetup(it, robertManager)
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasShownRegisterAlert = savedInstanceState?.getBoolean(SAVE_INSTANCE_HAS_SHOWN_REGISTER_ALERT) ?: hasShownRegisterAlert

        setFragmentResultListener(UniversalQrScanFragment.SCANNED_CODE_RESULT_KEY) { _, bundle ->
            val scannedData = bundle.getString(UniversalQrScanFragment.SCANNED_CODE_BUNDLE_KEY)
            if (scannedData?.isValidUUID() == true) {
                "$REPORT_UUID_DEEPLINK$scannedData"
            } else {
                scannedData
            }?.let { data ->
                if (URLUtil.isValidUrl(data)) {
                    val uri = Uri.parse(data).buildUpon()
                        .appendQueryParameter(DeeplinkManager.DEEPLINK_CERTIFICATE_ORIGIN_PARAMETER, DeeplinkOrigin.UNIVERSAL.name)
                        .build()
                    val finalUri = DeeplinkManager.transformFragmentToCodeParam(uri)
                    (activity as? MainActivity)?.processDeeplink(finalUri)
                } else {
                    lifecycleScope.launch {
                        findNavControllerOrNull()
                            ?.safeNavigate(
                                HomeFragmentDirections.actionHomeFragmentToNavWallet(
                                    code = data,
                                    deeplinkOrigin = DeeplinkOrigin.UNIVERSAL,
                                )
                            )
                    }
                }
            }
        }
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                refreshScreen()
            }
        }

        permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            if (isGranted.all { it.value }) {
                activateProximity()
            } else if (
                ProximityManager.getManifestProximityPermissions().any { permission -> !shouldShowRequestPermissionRationale(permission) }
            ) {
                context?.showPermissionSettingsDialog(
                    strings = strings,
                    messageKey = ProximityManager.getProximityPermissionExplanationKey(),
                    positiveKey = "common.settings",
                    neutralKey = "common.readMore",
                    cancelable = true,
                    positiveAction = {
                        openAppSettings()
                    },
                    neutralAction = {
                        strings["common.privacyPolicy"]?.openInExternalBrowser(requireContext())
                    },
                    negativeAction = null
                )
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        fragmentRecyclerViewFabBinding = view?.let { FragmentRecyclerViewFabBinding.bind(it) }

        activity?.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        activity?.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        activity?.registerReceiver(errorReceiver, IntentFilter(Constants.Notification.SERVICE_ERROR))

        getActivityBinding()?.errorLayout?.let { errorLayout ->
            errorLayout.post {
                errorLayout.translationY = errorLayout.height.toFloat()
            }
        }

        isProximityOn = ProximityManager.isProximityOn(requireContext(), robertManager)

        if (arguments?.getBoolean(START_PROXIMITY_ARG_KEY) == true) {
            arguments?.remove(START_PROXIMITY_ARG_KEY)
            activateProximity()
        }

        setupExtendedFab()

        viewLifecycleOwner.lifecycleScope.launch {
            val isLowStorage = context?.isLowStorage() ?: false
            if (isLowStorage && !sharedPrefs.lowStorageAlertShown) {
                sharedPrefs.lowStorageAlertShown = true
                findNavControllerOrNull()
                    ?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToLowStorageBottomSheetFragment())
            } else if (!isLowStorage) {
                sharedPrefs.lowStorageAlertShown = false
            }
        }

        return view
    }

    private fun setupExtendedFab() {
        fragmentRecyclerViewFabBinding?.floatingActionButton?.let { fab ->
            fab.text = strings["home.qrScan.button.title"]
            fab.setOnClickListener {
                if (fab.isExtended) {
                    analyticsManager.reportAppEvent(AppEventName.e19, null)
                } else {
                    analyticsManager.reportAppEvent(AppEventName.e18, null)
                }
                findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToUniversalQrScanFragment())
            }
            binding?.recyclerView?.addOnScrollListener(ExtendedFloatingActionButtonScrollListener(fab))
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
            refreshItems(
                context?.let {
                    ProximityManager.getDeviceSetup(it, robertManager)
                }
            )
        }
        viewModel.clearDataSuccess.observe(viewLifecycleOwner) {
            activateProximity()
        }
        viewModel.activateProximitySuccess.observe(viewLifecycleOwner) {
            refreshItems(
                context?.let {
                    ProximityManager.getDeviceSetup(it, robertManager)
                }
            )
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
        infoCenterManager.infos.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        infoCenterManager.strings.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        keyFiguresManager.figures.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.activeAttestationCount.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.favoriteDcc.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.venuesQrCodeLiveData.observe(viewLifecycleOwner) { venues ->
            refreshScreen()
            showRegisterRequiredIfNeeded(venues)
        }
        viewModel.smartWalletMap.collectDataWithLifecycle(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override fun refreshScreen() {
        fragmentRecyclerViewFabBinding?.floatingActionButton?.text = strings["home.qrScan.button.title"]
        super.refreshScreen()
    }

    private fun showRegisterRequiredIfNeeded(venues: List<VenueQrCode>) {
        if (!hasShownRegisterAlert && !robertManager.isRegistered && venues.isNotEmpty()) {
            context?.let {
                MaterialAlertDialogBuilder(it)
                    .setTitle(strings["robertStatus.error.alert.title"])
                    .setMessage(strings["robertStatus.error.alert.message"])
                    .setPositiveButton(strings["robertStatus.error.alert.action"]) { _, _ ->
                        findNavControllerOrNull()
                            ?.safeNavigate(
                                HomeFragmentDirections.actionHomeFragmentToCaptchaFragment(
                                    CaptchaNextFragment.Back,
                                    null,
                                )
                            )
                    }
                    .setNegativeButton(strings["robertStatus.error.alert.later"], null)
                    .setCancelable(false)
                    .show()
                hasShownRegisterAlert = true
            }
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

    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        val notification = robertManager.configuration.notification
        if (notification != null && sharedPrefs.notificationVersionClosed < notification.version) {
            addNotificationItem(items, notification)
        }

        val isSick = robertManager.isSick
        val showAsSick = isSick || robertManager.isImmune

        val deviceSetup = context?.let {
            ProximityManager.getDeviceSetup(it, robertManager)
        }

        addTopImageItems(items)
        addSubTitleItem(items)
        addActivateButtonItems(items, deviceSetup)

        addSectionSeparator(items)

        // Wallet
            addWalletItems(items)
            addSectionSeparator(items)

        // Health items
        addHealthItems(items, showAsSick)
        addSectionSeparator(items)

        // Contact tracing items
        addContactTracingSection(items, showAsSick)
        if (robertManager.configuration.displayIsolation) {
            addIsolationItems(items, analyticsManager)
        }
        addDeclareItems(items)
        addSectionSeparator(items)

        // Vaccination items
        addVaccinationSectionItems(items)
        if (robertManager.configuration.displayVaccination) {
            addVaccinationItems(items)
        }
        addUrgentDgsItems(items)
        addSectionSeparator(items)

        // Venue items
            addVenueItems(items, isSick)
            addSectionSeparator(items)

        // Attestation
        if (robertManager.configuration.displayAttestation) {
            addAttestationItems(items)
            addSectionSeparator(items)
        }

        addFiguresItems(items)
        addSectionSeparator(items)
        // News items
        addNewsItems(items)

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
                        ConfigConstant.Store.TAC_WEBSITE.openInExternalBrowser(requireContext())
                    }
                }
            }
        }
    }

    private fun addNotificationItem(items: ArrayList<GenericItem>, notification: Configuration.Notification) {
        val title = strings[notification.title] ?: return
        val subtitle = strings[notification.subtitle] ?: return
        val url = strings[notification.url] ?: return

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = R.drawable.ic_notif_envelope.toLong()
        }

        items += cardWithActionItem {
            mainTitle = title
            mainBody = subtitle
            mainImage = R.drawable.ic_notif_envelope
            mainLayoutDirection = LayoutDirection.LTR
            onCardClick = {
                url.openInExternalBrowser(requireContext())
            }
            onDismissClick = {
                sharedPrefs.notificationVersionClosed = notification.version
                refreshScreen()
            }
            identifier = R.drawable.ic_notif_envelope.toLong() + 1
        }
    }

    private fun addTopImageItems(items: ArrayList<GenericItem>) {
        onOffLottieItem = onOffLottieItem {
            identifier = R.id.item_on_off_lottie.toLong()
        }
        logoItem = logoItem {
            identifier = R.id.item_logo.toLong()
        }
        if (isAnimationEnabled()) {
            onOffLottieItem?.let { items += it }
        } else {
            logoItem?.let { items += it }
        }
    }

    fun addSubTitleItem(items: ArrayList<GenericItem>) {
        subTitleItem = titleItem {
            onClick = {
                findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToTuneProximityFragment())
            }
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += subTitleItem
    }

    private fun addActivateButtonItems(items: ArrayList<GenericItem>, deviceSetup: DeviceSetup?) {
        proximityButtonItem = proximityButtonItem {
            mainText = strings["home.mainButton.activate"]
            lightText = strings["home.mainButton.deactivate"]
            onClickListener = View.OnClickListener {
                onProximityButtonClick(deviceSetup)
            }
            identifier = "home.mainButton.activate".hashCode().toLong()
        }

        when {
            robertManager.isRegistered -> {
                proximityButtonItem?.let { items += it }
            }
            !ProximityManager.isAdvertisingValid(robertManager, requireContext()) -> {
                items += cardWithActionItem(CardTheme.Primary) {
                    mainBody = strings["proximityController.error.noAdvertising"]
                    identifier = "proximityController.error.noAdvertising".hashCode().toLong()
                }
            }
            else -> {
                items += cardWithActionItem(CardTheme.Primary) {
                    mainBody = strings["home.activationExplanation"]
                    onCardClick = {
                        onProximityButtonClick(deviceSetup)
                    }
                    identifier = "home.activationExplanation".hashCode().toLong()
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
                ProximityManager.hasUnstableBluetooth(requireContext()) -> {
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

    private fun addVaccinationSectionItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.vaccinationSection.title"]
            identifier = "home.vaccinationSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    private fun addUrgentDgsItems(items: ArrayList<GenericItem>) {
        if (robertManager.configuration.displayUrgentDgs) {
            items += cardWithActionItem {
                onCardClick = {
                    findNavControllerOrNull()?.safeNavigate(
                        HomeFragmentDirections.actionHomeFragmentToUrgentInfoFragment()
                    )
                }
                mainImage = R.drawable.ic_dgsurgent
                mainTitle = strings["home.healthSection.dgsUrgent.title"]
                mainBody = strings["home.healthSection.dgsUrgent.subtitle"]
                identifier = "home.healthSection.dgsUrgent.title".hashCode().toLong()
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_medium
                identifier = items.count().toLong()
            }
        }
    }

    private fun addContactTracingSection(items: MutableList<GenericItem>, showAsSick: Boolean) {
        items += bigTitleItem {
            text = strings["home.contactSection.title"]
            identifier = "home.contactSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    private fun addHealthItems(items: ArrayList<GenericItem>, showAsSick: Boolean) {
        items += bigTitleItem {
            text = strings["home.healthSection.title"]
            identifier = "home.healthSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        when {
            showAsSick -> {
                healthItem = null
                items += cardWithActionItem(CardTheme.Sick) {
                    mainImage = R.drawable.health_card
                    mainTitle = strings["home.healthSection.isSick.standaloneTitle"]
                    onCardClick = {
                        analyticsManager.reportAppEvent(AppEventName.e5, null)
                        findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToIsSickFragment())
                    }
                    identifier = R.drawable.health_card.toLong()
                }
                items += spaceItem {
                    spaceRes = R.dimen.spacing_medium
                    identifier = items.count().toLong()
                }
            }
            !sharedPrefs.hideRiskStatus -> {
                healthItem = risksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.let {
                    cardWithActionItem(CardTheme.Color) {
                        mainImage = R.drawable.health_card
                        gradientBackground = it.getGradientBackground()
                        onCardClick = {
                            analyticsManager.reportAppEvent(AppEventName.e5, null)
                            findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToHealthFragment())
                        }
                        identifier = R.drawable.health_card.toLong()

                        actions = refreshStatusActions(robertManager.liveUpdatingRiskStatus.value?.peekContent())?.let { listOf(it) }
                    }
                }

                healthItem?.let { healthItem ->
                    items += healthItem
                    items += spaceItem {
                        spaceRes = R.dimen.spacing_medium
                        identifier = items.count().toLong()
                    }
                }
            }
        }
    }

    private fun addVenueItems(items: ArrayList<GenericItem>, isSick: Boolean) {
        items += bigTitleItem {
            text = strings["home.venuesSection.title"]
            identifier = "home.venuesSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += cardWithActionItem {
            mainImage = R.drawable.signal_card
            mainLayoutDirection = LayoutDirection.RTL
            onCardClick = {
                if (isSick) {
                    MaterialAlertDialogBuilder(requireContext()).showAlertSickVenue(
                        strings = strings,
                        onIgnore = { startRecordVenue() },
                        onCancel = null,
                    )
                } else {
                    startRecordVenue()
                }
            }
            mainTitle = strings["home.venuesSection.recordCell.title"]
            mainBody = strings["home.venuesSection.recordCell.subtitle"]
            identifier = R.drawable.signal_card.toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }

        items += cardWithActionItem {
            mainImage = R.drawable.parties_card
            onCardClick = {
                startPrivateEvent()
            }
            mainTitle = strings["home.venuesSection.privateCell.title"]
            mainBody = strings["home.venuesSection.privateCell.subtitle"]
            identifier = R.drawable.parties_card.toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun startRecordVenue() {
        analyticsManager.reportAppEvent(AppEventName.e12, null)
        findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToVenueQrCodeFragment())
    }

    private fun startPrivateEvent() {
        findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToVenuesPrivateEventFragment())
    }

    private fun addFiguresItems(items: ArrayList<GenericItem>) {
        val localKeyFiguresFetchError = (keyFiguresManager.figures.value?.peekContent() as? TacResult.Failure)?.throwable
        if (localKeyFiguresFetchError is KeyFiguresNotAvailableException) {
            addNoConnectionItem(items, localKeyFiguresFetchError)
        } else {
            items += bigTitleItem {
                text = strings["home.infoSection.keyFigures"]
                linkText = strings["home.figuresSection.all"]
                onClickLink = View.OnClickListener {
                    analyticsManager.reportAppEvent(AppEventName.e8, null)
                    findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToKeyFiguresFragment())
                }
                identifier = "home.infoSection.keyFigures".hashCode().toLong()
            }
            addKeyFiguresRecycler(items)
            addPostalCodeItem(items)
            addCompareChart(items)
        }
    }

    private fun addKeyFiguresRecycler(items: ArrayList<GenericItem>) {
        val darkMode = requireContext().isNightMode()
        items += horizontalRecyclerViewItem {
            viewPool = recyclerViewPool
            identifier = "keyfigureRecyclerview".hashCode().toLong()

            val arrayListItems = arrayListOf<GenericItem>()
            arrayListItems += spaceItem {
                spaceRes = R.dimen.spacing_large
                orientation = SpaceItem.Orientation.HORIZONTAL
            }
            keyFiguresManager.figures.value?.peekContent()?.data?.filter { it.isFeatured || it.isHighlighted == true }?.forEach {
                arrayListItems += homeScreeFigureCardItem {
                    strings[it.colorStringKey(darkMode)]?.let { colorBackground = Color.parseColor(it) }
                    onClick = View.OnClickListener { _ ->
                        analyticsManager.reportAppEvent(AppEventName.e8, null)
                        findNavControllerOrNull()?.safeNavigate(
                            HomeFragmentDirections.actionHomeFragmentToKeyFigureDetailsFragment(
                                it.labelKey
                            )
                        )
                    }
                    figureText = strings[it.labelShortStringKey]
                    identifier = it.labelShortStringKey.hashCode().toLong()
                    if (sharedPrefs.hasChosenPostalCode) {
                        valueText = it.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)
                            ?.valueToDisplay
                            ?.formatNumberIfNeeded(numberFormat) ?: it.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
                        regionText = it.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)?.dptLabel
                            ?: strings["common.country.france"]
                    } else {
                        valueText = it.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
                    }
                }
                arrayListItems += spaceItem {
                    spaceRes = R.dimen.spacing_medium
                    orientation = SpaceItem.Orientation.HORIZONTAL
                }
            }
            arrayListItems += spaceItem {
                spaceRes = R.dimen.spacing_medium
                orientation = SpaceItem.Orientation.HORIZONTAL
            }

            horizontalItems = arrayListItems
        }
    }

    private fun addNoConnectionItem(items: ArrayList<GenericItem>, localKeyFiguresFetchError: KeyFiguresNotAvailableException) {
        items += bigTitleItem {
            text = strings["home.infoSection.keyFigures"]
            identifier = "home.infoSection.keyFigures".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        items += explanationActionCardItem {
            explanation = localKeyFiguresFetchError.getString(strings)
            bottomText = strings["keyFiguresController.fetchError.button"]
            onClick = {
                viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                    (activity as? MainActivity)?.showProgress(true)
                    keyFiguresManager.onAppForeground(requireContext())
                    vaccinationCenterManager.postalCodeDidUpdate(
                        requireContext(),
                        sharedPrefs,
                        sharedPrefs.chosenPostalCode,
                    )
                    infoCenterManager.refreshIfNeeded(requireContext())
                    (activity as? MainActivity)?.showProgress(false)
                    refreshScreen()
                }
            }
            identifier = "keyFiguresController.fetchError.button".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun addPostalCodeItem(items: ArrayList<GenericItem>) {
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
                            ?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToPostalCodeBottomSheetFragment())
                    }
                    identifier = "common.updatePostalCode".hashCode().toLong()
                }
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_medium
                identifier = items.count().toLong()
            }
        }
    }

    private fun addCompareChart(items: ArrayList<GenericItem>) {
        items += compareFigureCardChartItem {
            shareContentDescription = strings["accessibility.hint.keyFigure.chart.share"]
            chartExplanationLabel = strings["home.figuresSection.keyFigures.chart.footer"]
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToCompareKeyFiguresFragment())
            }
            onShareCard = { binding ->
                ShareManager.shareChart(
                    this@HomeFragment,
                    binding
                )
            }
            chartData = {
                val minDate = System.currentTimeMillis() / 1000 - ChartManager.ChartRange.ALL.rangeSec
                keyFiguresManager.figures.value?.peekContent()?.let { keyFiguresResult ->

                    val keyFigure1 = keyFiguresResult.data?.firstOrNull {
                        it.labelKey == sharedPrefs.keyFigureCompare1
                    } ?: keyFiguresResult.data?.firstOrNull {
                        // Fallback to config KeyFigures if the selected figures does not exist anymore
                        it.labelKey == robertManager.configuration.getDefaultFigureLabel1()
                    }
                    val keyFigure2 = keyFiguresResult.data?.firstOrNull {
                        it.labelKey == sharedPrefs.keyFigureCompare2
                    } ?: keyFiguresResult.data?.firstOrNull {
                        // Fallback to config KeyFigures if the selected figures does not exist anymore
                        it.labelKey == robertManager.configuration.getDefaultFigureLabel2()
                    }

                    CompareFigureChartItem.ChartCompareFiguresData(
                        keyFigure1?.let { key1 ->
                            keyFigure2?.let { key2 ->
                                context?.let { context ->
                                    val pair = Pair(key1, key2)
                                    pair.generateCombinedData(context, strings, minDate)
                                }
                            }
                        },
                        keyFigure1?.magnitude == keyFigure2?.magnitude
                    )
                }
            }
            isChartAnimated = false
            identifier = "home.figuresSection.keyFigures.chart.footer".hashCode().toLong()
        }
    }

    private fun addNewsItems(items: ArrayList<GenericItem>) {
        val infoCenterStrings = infoCenterManager.strings.value?.peekContent() ?: emptyMap()
        val infoCenterInfos = infoCenterManager.infos.value?.peekContent()?.take(NUMBER_NEWS_DISPLAYED)

        if (infoCenterStrings.isNotEmpty() && infoCenterInfos != null) {
            items += bigTitleItem {
                text = strings["home.infoSection.news"]
                linkText = strings["home.infoSection.all"]
                onClickLink = View.OnClickListener {
                    analyticsManager.reportAppEvent(AppEventName.e10, null)
                    findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToInfoCenterFragment())
                }
                identifier = "home.infoSection.news".hashCode().toLong()
            }
            items += horizontalRecyclerViewItem {
                val arrayListItems = arrayListOf<GenericItem>()
                arrayListItems += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    orientation = SpaceItem.Orientation.HORIZONTAL
                }
                infoCenterInfos.forEach { info ->
                    arrayListItems += homeScreeInfoCardItem {
                        titleText = infoCenterStrings[info.titleKey]
                        captionText = info.timestamp.seconds.getRelativeDateTimeString(requireContext(), strings)
                        subtitleText = infoCenterStrings[info.descriptionKey]
                        identifier = info.titleKey.hashCode().toLong()
                        onClick = View.OnClickListener {
                            analyticsManager.reportAppEvent(AppEventName.e10, null)
                            findNavControllerOrNull()?.safeNavigate(
                                HomeFragmentDirections.actionHomeFragmentToInfoCenterFragment(
                                    info.titleKey.hashCode().toLong()
                                )
                            )
                        }
                    }
                    arrayListItems += spaceItem {
                        spaceRes = R.dimen.spacing_medium
                        orientation = SpaceItem.Orientation.HORIZONTAL
                    }
                }
                arrayListItems += spaceItem {
                    spaceRes = R.dimen.spacing_medium
                    orientation = SpaceItem.Orientation.HORIZONTAL
                }

                viewPool = recyclerViewPool
                horizontalItems = arrayListItems
                identifier = "infosRecyclerView".hashCode().toLong()
            }

            addSectionSeparator(items)
        }
    }

    private fun addWalletItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.walletSection.title"]
            identifier = "home.walletSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val favoriteDcc = viewModel.favoriteDcc.value?.peekContent()
        if (favoriteDcc != null) {
            items += smallQrCodeCardItem {
                val qrCodeSize = R.dimen.card_image_height.toDimensSize(requireContext()).toInt()
                title = strings["home.walletSection.favoriteCertificate.cell.title"]
                body = strings["home.walletSection.favoriteCertificate.cell.subtitle"]
                generateBarcode = {
                    barcodeEncoder.encodeBitmap(
                        favoriteDcc.value,
                        BarcodeFormat.QR_CODE,
                        qrCodeSize,
                        qrCodeSize,
                    )
                }
                onClick = {
                    findNavControllerOrNull()?.safeNavigate(
                        HomeFragmentDirections.actionHomeFragmentToNavWallet(
                            navCertificateId = favoriteDcc.id
                        )
                    )
                }
                identifier = favoriteDcc.id.hashCode().toLong()
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_medium
                identifier = items.count().toLong()
            }
        }

        val walletState = viewModel.getWalletState()
        items += cardWithActionItem(walletState.theme) {
            mainImage = walletState.icon
            mainLayoutDirection = LayoutDirection.RTL
            onCardClick = {
                findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToNavWallet())
            }
            mainTitle = strings["home.attestationSection.sanitaryCertificates.cell.title"]
            mainBody = strings[walletState.bodyKey]
            identifier = R.drawable.wallet_card.toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun addAttestationItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.attestationsSection.title"]
            identifier = "home.attestationsSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += cardWithActionItem {
            mainImage = R.drawable.attestation_card
            onCardClick = {
                analyticsManager.reportAppEvent(AppEventName.e11, null)
                findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToAttestationsFragment())
            }
            mainTitle = strings["home.attestationSection.cell.title"]
            val attestationCount = viewModel.activeAttestationCount.value?.peekContent()
            mainBody = when (attestationCount) {
                0, null -> strings["home.attestationSection.cell.subtitle.noAttestations"]
                1 -> strings["home.attestationSection.cell.subtitle.oneAttestation"]
                else -> stringsFormat("home.attestationSection.cell.subtitle.multipleAttestations", attestationCount)
            }
            identifier = R.drawable.attestation_card.toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun showPostalCodeDialog() {
        MaterialAlertDialogBuilder(requireContext()).showPostalCodeDialog(
            layoutInflater,
            strings,
            this,
            sharedPrefs
        )
    }

    private fun addDeclareItems(items: ArrayList<GenericItem>) {
        items += cardWithActionItem {
            mainTitle = strings["home.declareSection.cellTitle"]
            mainBody = strings["home.declareSection.cellSubtitle"]
            mainImage = R.drawable.declare_card
            contentDescription = strings["home.declareSection.title"]
            onCardClick = {
                findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToReportFragment())
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
            mainTitle = strings["home.vaccinationSection.cellTitle"]
            mainLayoutDirection = LayoutDirection.RTL
            mainTitleColorRes = R.color.color_no_risk
            mainImage = R.drawable.ic_vaccine_center
            mainTitleIcon = R.drawable.ic_vaccin
            mainBody = strings["home.vaccinationSection.cellSubtitle"]
            contentDescription = strings["home.vaccinationSection.cellTitle"]
            onCardClick = {
                analyticsManager.reportAppEvent(AppEventName.e7, null)
                findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToVaccinationFragment())
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
                Action(R.drawable.ic_compass, strings["proximityController.tuneProximity"]) {
                    findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToTuneProximityFragment())
                },
                Action(R.drawable.ic_link, strings["home.moreSection.usefulLinks"]) {
                    findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToLinksFragment())
                },
                Action(R.drawable.ic_history, strings["home.moreSection.venuesHistory"]) {
                    findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToVenuesHistoryFragment())
                }.takeIf {
                    true ||
                    robertManager.configuration.displayRecordVenues
                        || !viewModel.venuesQrCodeLiveData.value.isNullOrEmpty()
                },
                Action(R.drawable.ic_2d_doc, strings["home.moreSection.verifySanitaryCertificate"]) {
                    findNavControllerOrNull()?.safeNavigate(
                        HomeFragmentDirections.actionHomeFragmentToVerifyWalletQRCodeFragment()
                    )
                },
                Action(R.drawable.ic_settings, strings["common.settings"]) {
                    findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToSettingsFragment())
                },
                Action(R.drawable.ic_about, strings["home.moreSection.aboutStopCovid"]) {
                    findNavControllerOrNull()?.safeNavigate(
                        R.id.nav_about, null,
                        navOptions {
                            anim {
                                enter = R.anim.nav_default_enter_anim
                                popEnter = R.anim.nav_default_pop_enter_anim
                                popExit = R.anim.nav_default_pop_exit_anim
                                exit = R.anim.nav_default_exit_anim
                            }
                        }
                    )
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
            robertManager.isImmune -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["home.activation.sick.alert.title"])
                    .setMessage(strings["home.activation.sick.alert.message"])
                    .setPositiveButton(strings["common.ok"], null)
                    .show()
            }
            !robertManager.isRegistered -> {
                viewLifecycleOwnerOrNull()?.lifecycleScope?.launch(Dispatchers.IO) {
                    if (viewModel.refreshConfig(requireContext().applicationContext as RobertApplication)) {
                        withContext(Dispatchers.Main) {
                            findNavControllerOrNull()
                                ?.safeNavigate(
                                    HomeFragmentDirections.actionHomeFragmentToCaptchaFragment(
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
        if (showReminder && sharedPrefs.showActivationReminderDialog) {
            findNavControllerOrNull()?.safeNavigate(HomeFragmentDirections.actionHomeFragmentToReminderDialogFragment())
        }
    }

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

            proximityButtonItem?.showMainButton = !isProximityOn
            if (deviceSetup != DeviceSetup.BLE) {
                proximityButtonItem?.onClickListener = getErrorClickListener()
            }

            if (isAdded && !isHidden) {
                refreshTitleAndErrorLayout()
            }

            subTitleItem.text = if (isProximityOn) {
                strings["proximityController.switch.subtitle.activated"] + "\n" +
                strings["proximityController.switch.subtext.activated"]
            } else {
                strings["proximityController.switch.subtitle.deactivated"] + "\n" +
                strings["proximityController.switch.subtext.deactivated"]
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
        appCompatActivity?.supportActionBar?.title = strings[getTitleKey()]
        asyncUpdateNewVersionAvailableTitle()
        if (sharedPrefs.showErrorPanel) {
            refreshErrorLayout()
        }
    }

    private fun refreshErrorLayout() {
        context?.let { context ->
            updateErrorLayout(getActivityBinding(), ProximityManager.getDeviceSetup(context, robertManager))
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

    private fun refreshHealthItem(context: Context) {
        healthItem?.apply {
            risksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.let {
                mainTitle = strings[it.labels.homeTitle]
                mainBody = strings[it.labels.homeSub]

                mainHeader = getStatusLastUpdateToDisplay(context, robertManager.atRiskLastRefresh, it.riskLevel)
            }

            actions = refreshStatusActions(robertManager.liveUpdatingRiskStatus.value?.peekContent())?.let { listOf(it) }
        }
    }

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

    private fun updateErrorLayout(activityMainBinding: ActivityMainBinding?, deviceSetup: DeviceSetup?) {
        activityMainBinding?.errorTextView?.text = ProximityManager.getErrorText(this, robertManager, currentServiceError, strings)
        val clickListener = getErrorClickListener()

        // refresh padding in case error text length change
        activityMainBinding?.errorLayout?.let { errorLayout ->
            if (errorLayout.isVisible) {
                errorLayout.post {
                    binding?.root?.updatePadding(bottom = errorLayout.height)
                }
            }
        }

        getActivityBinding()?.errorTextView?.setOnClickListener(clickListener)
        if (clickListener != null) {
            getActivityBinding()?.errorTextView?.addRipple()
        } else {
            getActivityBinding()?.errorTextView?.background = null
        }
        if (getActivityBinding()?.errorTextView?.text.isNullOrEmpty()) {
            hideErrorLayout(activityMainBinding)
        } else if (deviceSetup != DeviceSetup.NO_BLE) {
            showErrorLayout(activityMainBinding)
        }
    }

    private fun getErrorClickListener() : View.OnClickListener? {
        val clickListener = ProximityManager.getErrorClickListener(
            this,
            robertManager,
            activityResultLauncher,
            currentServiceError,
            permissionResultLauncher,
            {
                if (SystemClock.elapsedRealtime() > proximityClickThreshold) {
                    proximityClickThreshold = SystemClock.elapsedRealtime() + PROXIMITY_BUTTON_DELAY
                    activateProximity()
                }
            },
        ) {
            viewLifecycleOwnerOrNull()?.lifecycleScope?.launch(Dispatchers.Main) {
                deactivateProximity(false)
                delay(PROXIMITY_BUTTON_DELAY)
                activateProximity()
            }
        }
        return clickListener
    }

    private fun hideErrorLayout(activityMainBinding: ActivityMainBinding?) {
        activityMainBinding?.errorLayout?.let { errorLayout ->
            if (!hideErrorLayoutAnimationInProgress && (showErrorLayoutAnimationInProgress || errorLayout.isVisible)) {
                hideErrorLayoutAnimationInProgress = true
                activityMainBinding.errorTextView.isClickable = false
                errorLayout.animate()?.apply {
                    duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                    interpolator = this@HomeFragment.interpolator
                    setUpdateListener { valueAnimator ->
                        binding?.root
                            ?.updatePadding(bottom = (errorLayout.height.toFloat() * (1f - valueAnimator.animatedValue as Float)).toInt())
                    }
                    translationY(errorLayout.height.toFloat())
                    withEndAction {
                        errorLayout.isInvisible = true
                        binding?.recyclerView?.let {
                            getAppBarLayout()?.refreshLift(it)
                        }
                        hideErrorLayoutAnimationInProgress = false
                    }
                    start()
                }
            }
        }
    }

    private fun showErrorLayout(activityMainBinding: ActivityMainBinding?) {
        activityMainBinding?.errorLayout?.let { errorLayout ->
            if (!showErrorLayoutAnimationInProgress
                && (hideErrorLayoutAnimationInProgress || errorLayout.isInvisible || errorLayout.translationY != 0f)
            ) {
                showErrorLayoutAnimationInProgress = true
                errorLayout.post {
                    context?.let {
                        errorLayout.isVisible = true
                        errorLayout.animate().apply {
                            duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
                            interpolator = this@HomeFragment.interpolator
                            setUpdateListener { valueAnimator ->
                                binding?.root
                                    ?.updatePadding(bottom = (errorLayout.height.toFloat() * valueAnimator.animatedValue as Float).toInt())
                            }
                            translationY(0f)
                            withEndAction {
                                activityMainBinding.errorTextView.isClickable = true
                                showErrorLayoutAnimationInProgress = false
                            }
                            start()
                        }
                    }
                }
            }
        }
    }

    private fun setTopBarOnclick() {
        getActivityBinding()?.toolbar?.setOnClickListener {
            downloadLatest()
        }
    }

    private fun asyncUpdateNewVersionAvailableTitle() {
        CoroutineScope(Dispatchers.Default).launch {
            val indicatorText = newVersionAvailableIndicator()
            if (indicatorText != "")
            CoroutineScope(Dispatchers.Main).launch {
                val titleText = strings[getTitleKey()] + "  " + indicatorText
                (activity as AppCompatActivity).supportActionBar?.title = titleText
                setTopBarOnclick()
            }
        }
    }

    private fun newVersionAvailableIndicator(): String {
        return when {
            updateAvailable() -> ":: 🎁 ::"
            else -> ""
        }
    }

    private fun updateAvailable(): Boolean {
        try {
            return ! AboutFragment.isLatest()
        }
        catch(e: Exception) {
            return false
        }
    }

    private fun toast(text: String?) {
        toast(requireContext(), text)
    }

    private fun downloadLatest() {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                toast(strings["aboutController.toast.downloadingLatest"])
                AboutFragment.Downloader(requireContext()).fetch()
            }.onFailure {
                toast(strings["aboutController.toast.errorFetchingUpdates"])
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVE_INSTANCE_HAS_SHOWN_REGISTER_ALERT, hasShownRegisterAlert)
    }

    override fun onDestroyView() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        activity?.unregisterReceiver(receiver)
        activity?.unregisterReceiver(errorReceiver)
        super.onDestroyView()
    }

    companion object {
        private const val PROXIMITY_BUTTON_DELAY: Long = 2000L
        const val START_PROXIMITY_ARG_KEY: String = "START_PROXIMITY_ARG_KEY"
        private const val REPORT_UUID_DEEPLINK: String = "https://bonjour.tousanticovid.gouv.fr/app/code/"
        private const val NUMBER_NEWS_DISPLAYED: Int = 4

        private const val SAVE_INSTANCE_HAS_SHOWN_REGISTER_ALERT: String = "SAVED_STATE_HAS_SHOWN_REGISTER_ALERT_KEY"
    }
}
