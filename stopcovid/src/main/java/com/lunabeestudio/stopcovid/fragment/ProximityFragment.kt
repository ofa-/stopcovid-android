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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.preference.PreferenceManager
import com.airbnb.lottie.utils.Utils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.model.AtRiskStatus
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.utils.EventObserver
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.addRipple
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.extension.resolveAttribute
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.formatNumberIfNeeded
import com.lunabeestudio.stopcovid.extension.getDepartmentLabel
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.hasChosenPostalCode
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showPostalCodeDialog
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.fastitem.ImageBackgroundCardItem
import com.lunabeestudio.stopcovid.fastitem.InfoCenterCardItem
import com.lunabeestudio.stopcovid.fastitem.LogoItem
import com.lunabeestudio.stopcovid.fastitem.LottieItem
import com.lunabeestudio.stopcovid.fastitem.NumbersCardItem
import com.lunabeestudio.stopcovid.fastitem.ProximityButtonItem
import com.lunabeestudio.stopcovid.fastitem.State
import com.lunabeestudio.stopcovid.fastitem.addPostalCodeCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.coreui.fastitem.CaptionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.TitleItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.fastitem.buttonCardItem
import com.lunabeestudio.stopcovid.fastitem.imageBackgroundCardItem
import com.lunabeestudio.stopcovid.fastitem.infoCenterCardItem
import com.lunabeestudio.stopcovid.fastitem.linkCardItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.lottieItem
import com.lunabeestudio.stopcovid.fastitem.moreCardItem
import com.lunabeestudio.stopcovid.fastitem.numbersCardItem
import com.lunabeestudio.stopcovid.fastitem.proximityButtonItem
import com.lunabeestudio.stopcovid.manager.InfoCenterManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.model.DeviceSetup
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModel
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

class ProximityFragment : TimeMainFragment() {

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: ProximityViewModel by viewModels {
        ProximityViewModelFactory(robertManager, requireContext().secureKeystoreDataSource())
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val deviceSetup by lazy {
        ProximityManager.getDeviceSetup(requireContext())
    }

    private lateinit var subTitleItem: TitleItem

    private var lottieItem: LottieItem? = null
    private var logoItem: LogoItem? = null
    private var proximityButtonItem: ProximityButtonItem? = null
    private lateinit var infoCenterCardItem: InfoCenterCardItem
    private lateinit var numbersCardItem: NumbersCardItem
    private var healthItem: ImageBackgroundCardItem? = null
    private var shouldRefresh: Boolean = false
    private var isProximityOn: Boolean = false
    private val interpolator = DecelerateInterpolator()
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            refreshItems()
        }
    }
    private var proximityClickThreshold = 0L
    private var lastAdapterRefresh: Long = 0L

    private val proximityServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val boundedService = (service as ProximityService.ProximityBinder).getService()

            val handleError = { error: RobertException ->
                context?.let {
                    viewModel.covidException.postValue(error.toCovidException())
                } ?: Unit
            }

            boundedService.consumeLastError()?.let(handleError)
            boundedService.onError = handleError
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    override fun getTitleKey(): String {
        //return "proximityController.tabBar.title"
        return when {
            deviceSetup == DeviceSetup.NO_BLE -> "app.name"
            ProximityManager.isProximityOn(requireContext(), robertManager) && deviceSetup == DeviceSetup.BLE -> "home.title.activated"
            else -> "home.title.deactivated"
        }
    }

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Constants.SharedPrefs.HAS_NEWS) {
            refreshItems()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        activity?.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        activity?.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

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

            val shortcut = builder.build()

            shortcutManager.setDynamicShortcuts(listOf(shortcut))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
        initHasNewsObserver()

        shouldRefresh = false

        findNavController().currentBackStackEntry?.savedStateHandle
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
                showErrorSnackBar(it.getString(strings))
            }
            refreshItems()
        }
        viewModel.refreshConfigSuccess.observe(viewLifecycleOwner) {
            findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToCaptchaFragment())
        }
        viewModel.activateProximitySuccess.observe(viewLifecycleOwner) {
            refreshItems()
            (requireContext().applicationContext as StopCovid).cancelReminder()
        }
        robertManager.atRiskStatus.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) { isAtRisk ->
            when {
                isAtRisk == AtRiskStatus.UNKNOWN -> {
                    healthItem = null
                    refreshScreen()
                }
                healthItem == null -> refreshScreen()
                isAtRisk == AtRiskStatus.AT_RISK -> refreshHealthItemAsync(requireContext(), pIsAtRisk = true, notifyAdapter = false)
                isAtRisk == AtRiskStatus.NOT_AT_RISK -> refreshHealthItemAsync(requireContext(), pIsAtRisk = false, notifyAdapter = false)
            }
        })
        InfoCenterManager.infos.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) {
            refreshScreen()
        })
        InfoCenterManager.strings.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) {
            refreshScreen()
        })
        KeyFiguresManager.figures.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) {
            refreshScreen()
        })
        viewModel.activeAttestationCount.observe(viewLifecycleOwner) {
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
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        val isSick = robertManager.isSick

        addTopImageItems(items)
        addSubTitleItem(items)
        addActivateButtonItems(items)
        addHealthItems(items, isSick)
        addNewsItems(items)
        if (robertManager.displayAttestation) {
            addAttestationItems(items)
        }
        addDeclareItems(items, isSick)
        addSharingItems(items)
        addMoreItems(items)

        refreshItems()

        return items
    }

    fun addSubTitleItem(items: ArrayList<GenericItem>) {
        subTitleItem = titleItem {
            onClick = {
                findNavController().navigate(ProximityFragmentDirections.actionProximityFragmentToTuneProximityFragment())
            }
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += subTitleItem
    }

    private fun addTopImageItems(items: ArrayList<GenericItem>) {
        if (deviceSetup != DeviceSetup.NO_BLE) {
            lottieItem = lottieItem {
                identifier = items.count().toLong()
            }
            logoItem = logoItem {
                identifier = items.count().toLong()
            }
            if (isAnimationEnabled()) {
                lottieItem?.let { items += it }
            } else {
                logoItem?.let { items += it }
            }
        }
    }

    private fun addActivateButtonItems(items: ArrayList<GenericItem>) {
        if (deviceSetup != DeviceSetup.NO_BLE) {
            proximityButtonItem = proximityButtonItem {
                mainText = strings["home.mainButton.activate"]
                lightText = strings["home.mainButton.deactivate"]
                onClickListener = View.OnClickListener {
                    if (SystemClock.elapsedRealtime() > proximityClickThreshold) {
                        proximityClickThreshold = SystemClock.elapsedRealtime() + PROXIMITY_BUTTON_DELAY
                        if (robertManager.isProximityActive) {
                            deactivateProximity()
                            refreshItems()
                        } else {
                            activateProximity()
                        }
                    }
                }
                identifier = items.count().toLong()
            }
            if (robertManager.isRegistered) {
                proximityButtonItem?.let { items += it }
            } else {
                items += buttonCardItem {
                    text = strings["home.activationExplanation"]
                    buttonText = strings["home.mainButton.activate"]
                    onClickListener = View.OnClickListener {
                        if (SystemClock.elapsedRealtime() > proximityClickThreshold) {
                            proximityClickThreshold = SystemClock.elapsedRealtime() + PROXIMITY_BUTTON_DELAY
                            if (robertManager.isProximityActive) {
                                deactivateProximity()
                                refreshItems()
                            } else {
                                activateProximity()
                            }
                        }
                    }
                    identifier = items.count().toLong()
                }
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
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
            deviceSetup == DeviceSetup.NO_BLE -> {
                items += imageBackgroundCardItem {
                    iconRes = R.drawable.health_card
                    backgroundDrawable = R.drawable.bg_no_risk
                    onClickListener = View.OnClickListener {
                        findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToHealthFragment())
                    }
                    title = strings["myHealthController.alert.atitudeToAdopt"]
                    subtitle = strings["home.healthSection.noContact.cellSubtitle"]
                    identifier = R.drawable.health_card.toLong()
                }
            }
            isSick -> {
                healthItem = null
                items += imageBackgroundCardItem {
                    iconRes = R.drawable.health_card
                    backgroundDrawable = R.drawable.bg_sick
                    title = strings["home.healthSection.isSick.standaloneTitle"]
                    subtitle = null
                    identifier = R.drawable.health_card.toLong()
                    onClickListener = View.OnClickListener {
                        findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToIsSickFragment())
                    }
                }
            }
            else -> {
                healthItem = this.healthItem ?: robertManager.isAtRisk?.let { atRisk ->
                    healthItem = imageBackgroundCardItem {
                        iconRes = R.drawable.health_card
                        backgroundDrawable = if (atRisk) {
                            R.drawable.bg_risk
                        } else {
                            R.drawable.bg_no_risk
                        }
                        onClickListener = View.OnClickListener {
                            findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToHealthFragment())
                        }
                        identifier = R.drawable.health_card.toLong()
                    }
                    healthItem
                }

                healthItem?.let { healthItem ->
                    items += healthItem
                }
            }
        }

        if (!isSick) {
            items += linkCardItem {
                label = strings["home.moreSection.testingSites"]
                iconRes = R.drawable.ic_search
                onClickListener = View.OnClickListener {
                    strings["myHealthController.testingSites.url"]?.openInExternalBrowser(requireContext())
                }
                identifier = label.hashCode().toLong()
            }

            items += linkCardItem {
                label = strings["myHealthController.covidAdvices.title"]
                iconRes = R.drawable.ic_baseline_chat_bubble_outline_24
                onClickListener = View.OnClickListener {
                    findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToInformationFragment())
                }
                identifier = label.hashCode().toLong()
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
    }

    private fun addNewsItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.infoSection.title"]
            identifier = items.count().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        numbersCardItem = numbersCardItem {
            header = strings["home.infoSection.keyFigures"]
            subheader = strings["keyfigure.dailyUpdates"]
            link = strings["home.infoSection.seeAll"]
            contentDescription = strings["home.infoSection.seeAll"]
            onClickListener = View.OnClickListener {
                findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToKeyFiguresFragment())
            }
            identifier = header.hashCode().toLong()
        }
        items += numbersCardItem

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        if (robertManager.displayDepartmentLevel) {
            if (sharedPrefs.chosenPostalCode == null) {
                items += addPostalCodeCardItem {
                    header = strings["home.infoSection.newPostalCode"]
                    content = strings["home.infoSection.newPostalCode.subtitle"]
                    link = strings["home.infoSection.newPostalCode.button"]
                    contentDescription = strings["home.infoSection.newPostalCode.subtitle"]
                    onClickListener = View.OnClickListener {
                        showPostalCodeDialog()
                    }
                    identifier = header.hashCode().toLong()
                }
            } else {
                items += linkCardItem {
                    label = strings["home.infoSection.updatePostalCode"]
                    iconRes = R.drawable.ic_map
                    onClickListener = View.OnClickListener {
                        findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToPostalCodeBottomSheetFragment())
                    }
                    identifier = label.hashCode().toLong()
                }
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
        }

        infoCenterCardItem = infoCenterCardItem {
            header = strings["home.infoSection.lastInfo"]
            link = strings["home.infoSection.readAll"]
            contentDescription = strings["home.infoSection.readAll"]
            showBadge = sharedPrefs.getBoolean(Constants.SharedPrefs.HAS_NEWS, false)
            onClickListener = View.OnClickListener {
                findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToInfoCenterFragment())
            }
            identifier = header.hashCode().toLong()
        }
        items += infoCenterCardItem

        items += linkCardItem {
            label = strings["home.moreSection.covidInfo"]
            iconRes = R.drawable.ic_web
            onClickListener = View.OnClickListener {
                strings["home.moreSection.covidInfo.url"]?.openInExternalBrowser(requireContext())
            }
            identifier = label.hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
    }

    private fun addAttestationItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.attestationSection.title"]
            identifier = items.count().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += imageBackgroundCardItem {
            iconRes = R.drawable.attestation_card
            backgroundDrawable = android.R.attr.colorPrimary.resolveAttribute(requireContext())
            onClickListener = View.OnClickListener {
                findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToAttestationsFragment())
            }
            title = strings["home.attestationSection.cell.title"]
            val attestationCount = viewModel.activeAttestationCount.value
            subtitle = when (attestationCount) {
                0, null -> strings["home.attestationSection.cell.subtitle.noAttestations"]
                1 -> strings["home.attestationSection.cell.subtitle.oneAttestation"]
                else -> stringsFormat("home.attestationSection.cell.subtitle.multipleAttestations", attestationCount)
            }
            identifier = R.drawable.attestation_card.toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
    }

    private fun showPostalCodeDialog() {
        MaterialAlertDialogBuilder(requireContext()).showPostalCodeDialog(
            layoutInflater,
            strings
        ) { postalCode ->
            sharedPrefs.chosenPostalCode = postalCode
            refreshScreen()
        }
    }

    private fun addDeclareItems(items: ArrayList<GenericItem>, isSick: Boolean) {
        if (deviceSetup != DeviceSetup.NO_BLE && !isSick) {
            items += bigTitleItem {
                text = strings["home.declareSection.title"]
                identifier = items.count().toLong()
                importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
            }

            items += imageBackgroundCardItem {
                title = strings["home.declareSection.cellTitle"]
                subtitle = strings["home.declareSection.cellSubtitle"]
                iconRes = R.drawable.declare_card
                contentDescription = strings["home.declareSection.title"]
                onClickListener = View.OnClickListener {
                    findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToReportFragment())
                }
                identifier = title.hashCode().toLong()
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
        }
    }

    private fun addSharingItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.sharingSection.title"]
            identifier = items.count().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += imageBackgroundCardItem {
            title = strings["home.sharingSection.cellTitle"]
            subtitle = strings["home.sharingSection.cellSubtitle"]
            contentDescription = strings["home.sharingSection.title"]
            iconRes = R.drawable.share_card
            onClickListener = View.OnClickListener {
                ShareCompat.IntentBuilder.from(requireActivity())
                    .setType("text/plain")
                    .setText(strings["sharingController.appSharingMessage"])
                    .startChooser()
            }
            identifier = title.hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
    }

    private fun addMoreItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.moreSection.title"]
            identifier = items.count().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += moreCardItem {

            proximityText = strings["proximityController.tuneProximity"]
            proximityOnClickListener = View.OnClickListener {
                findNavController().navigate(ProximityFragmentDirections.actionProximityFragmentToTuneProximityFragment())
            }
            proximityIconRes = R.drawable.ic_manage_data

            linksText = strings["home.moreSection.usefulLinks"]
            linksOnClickListener = View.OnClickListener {
                findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToLinksFragment())
            }
            linksIconRes = R.drawable.ic_link
            privacyText = strings["home.moreSection.privacy"]
            privacyOnClickListener = View.OnClickListener {
                findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToPrivacyFragment())
            }
            privacyIconRes = R.drawable.ic_privacy
            manageDataText = strings["common.settings"]
            manageDataOnClickListener = View.OnClickListener {
                findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToManageDataFragment())
            }
            manageDataIconRes = R.drawable.ic_manage_data
            aboutText = strings["home.moreSection.aboutStopCovid"]
            aboutOnClickListener = View.OnClickListener {
                findNavController().safeNavigate(R.id.nav_about, null, navOptions {
                    anim {
                        enter = R.anim.nav_default_enter_anim
                        popEnter = R.anim.nav_default_pop_enter_anim
                        popExit = R.anim.nav_default_pop_exit_anim
                        exit = R.anim.nav_default_exit_anim
                    }
                })
            }
            aboutIconRes = R.drawable.ic_about
            identifier = items.count().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
    }

    private fun activateProximity() {
        when {
            !robertManager.canActivateProximity -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["home.activation.sick.alert.title"])
                    .setMessage(strings["home.activation.sick.alert.message"])
                    .setPositiveButton(strings["common.ok"], null)
                    .show()
            }
            !robertManager.isRegistered -> {
                viewModel.refreshConfig(requireContext().applicationContext as RobertApplication)
            }
            else -> {
                bindToProximityService()
                viewModel.activateProximity(requireContext().applicationContext as RobertApplication)
            }
        }
    }

    private fun deactivateProximity() {
        robertManager.deactivateProximity(requireContext().applicationContext as RobertApplication)
        view?.rootView?.announceForAccessibility(strings["notification.proximityServiceNotRunning.title"])
        findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToReminderDialogFragment())
    }

    @OptIn(ExperimentalTime::class)
    @SuppressLint("RestrictedApi")
    private fun refreshItems() {
        if (strings.isEmpty()) {
            return // Do nothing until strings are loaded
        }

        refreshHealthItemAsync(requireContext(), pIsAtRisk = null, notifyAdapter = false)

        context?.let { context ->
            val freshProximityOn = ProximityManager.isProximityOn(context, robertManager)
            val wasProximityDifferent: Boolean = isProximityOn != freshProximityOn
            isProximityOn = freshProximityOn
            refreshTopImage(wasProximityDifferent)

            val infoCenterStrings = InfoCenterManager.strings.value?.peekContent() ?: emptyMap()

            InfoCenterManager.infos.value?.peekContent()?.firstOrNull()?.let { info ->
                infoCenterCardItem.apply {
                    subheader = info.timestamp.seconds.getRelativeDateTimeString(requireContext())
                    title = infoCenterStrings[info.titleKey]
                    subtitle = infoCenterStrings[info.descriptionKey]
                }
            }

            KeyFiguresManager.figures.value
                ?.peekContent()
                ?.filter { it.isFeatured }
                ?.takeIf { it.size > 2 }
                ?.let { keyFigures ->
                    numbersCardItem.apply {
                        val mode = if (requireContext().isNightMode()) {
                            DARK_STRING_KEY
                        } else {
                            LIGHT_STRING_KEY
                        }

                        franceData = NumbersCardItem.Data(
                            strings["common.country.france"],
                            generateFromKeyFigure(mode, keyFigures[0]),
                            generateFromKeyFigure(mode, keyFigures[1]),
                            generateFromKeyFigure(mode, keyFigures[2])
                        )

                        localData = if (sharedPrefs.hasChosenPostalCode) {
                            NumbersCardItem.Data(
                                keyFigures.getDepartmentLabel(sharedPrefs.chosenPostalCode),
                                generateFromKeyFigure(mode, keyFigures[0], true),
                                generateFromKeyFigure(mode, keyFigures[1], true),
                                generateFromKeyFigure(mode, keyFigures[2], true)
                            )
                        } else {
                            null
                        }
                    }
                }

            proximityButtonItem?.showMainButton = !isProximityOn
            proximityButtonItem?.isButtonEnabled = ProximityManager.getDeviceSetup(context) == DeviceSetup.BLE

            if (isAdded && !isHidden) {
                refreshTitleAndErrorLayout()
            }

            subTitleItem.text = if (isProximityOn) {
                strings["proximityController.switch.subtitle.activated"] + "\n" +
                strings["accessibility.hint.proximity.buttonState.activated"]
            } else {
                strings["proximityController.switch.subtitle.deactivated"] + "\n" +
                strings["accessibility.hint.proximity.buttonState.deactivated"]
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
            val freshProximityOn = ProximityManager.isProximityOn(context, robertManager)
            (activity as AppCompatActivity).supportActionBar?.title = strings[getTitleKey()]
            updateErrorLayout(getActivityBinding()?.errorLayout, freshProximityOn)
            asyncUpdateNewVersionAvailableTitle()
        }
    }

    private fun generateFromKeyFigure(mode: String, keyFigure: KeyFigure, fromDepartment: Boolean = false): NumbersCardItem.DataFigure {
        return NumbersCardItem.DataFigure(
            strings["${keyFigure.labelKey}.$SHORT_LABEL_STRING_KEY"],
            if (fromDepartment) {
                keyFigure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)?.valueToDisplay?.formatNumberIfNeeded(numberFormat)
            } else {
                keyFigure.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
            },
            strings["${keyFigure.labelKey}.$COLOR_CODE_STRING_KEY.$mode"]?.let { Color.parseColor(it) }
        )
    }

    private fun refreshTopImage(wasProximityDifferent: Boolean) {
        lottieItem?.state = if (wasProximityDifferent) {
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
    private fun refreshHealthItemAsync(context: Context, pIsAtRisk: Boolean?, notifyAdapter: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            healthItem?.apply {
                val isAtRisk = pIsAtRisk ?: robertManager.isAtRisk ?: return@launch

                header = stringsFormat(
                    "myHealthController.notification.update",
                    robertManager.atRiskLastRefresh?.milliseconds?.getRelativeDateTimeString(context)
                )

                if (isAtRisk) {
                    title = strings["home.healthSection.contact.cellTitle"]
                    subtitle = strings["home.healthSection.contact.cellSubtitle"]
                } else {
                    title = strings["home.healthSection.noContact.cellTitle"]
                    subtitle = strings["home.healthSection.noContact.cellSubtitle"]
                }

                backgroundDrawable = if (isAtRisk) {
                    R.drawable.bg_risk
                } else {
                    R.drawable.bg_no_risk
                }
            }

            if (notifyAdapter && binding?.recyclerView?.isComputingLayout == false) {
                withContext(Dispatchers.Main) {
                    lastAdapterRefresh = System.currentTimeMillis()
                    binding?.recyclerView?.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun timeRefresh() {
        val notifyCalledMoreThanHalfAMinuteAgo = System.currentTimeMillis() - lastAdapterRefresh > TimeUnit.SECONDS.toMillis(30)
        if (notifyCalledMoreThanHalfAMinuteAgo) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                healthItem?.apply {
                    header = stringsFormat(
                        "myHealthController.notification.update",
                        robertManager.atRiskLastRefresh?.milliseconds?.getRelativeDateTimeString(requireContext())
                    )
                }
                InfoCenterManager.infos.value?.peekContent()?.firstOrNull()?.let { info ->
                    infoCenterCardItem.apply {
                        subheader = info.timestamp.seconds.getRelativeDateTimeString(requireContext())
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

    private fun updateErrorLayout(errorLayout: FrameLayout?, freshProximityOn: Boolean) {
        getActivityBinding()?.errorTextView?.text = ProximityManager.getErrorText(this, robertManager, strings)
        val clickListener = ProximityManager.getErrorClickListener(this) {
            if (SystemClock.elapsedRealtime() > proximityClickThreshold) {
                proximityClickThreshold = SystemClock.elapsedRealtime() + PROXIMITY_BUTTON_DELAY
                activateProximity()
            }
        }
        getActivityBinding()?.errorTextView?.setOnClickListener(clickListener)
        if (clickListener != null) {
            getActivityBinding()?.errorTextView?.addRipple()
        } else {
            getActivityBinding()?.errorTextView?.background = null
        }
        if (freshProximityOn) {
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

    private fun asyncUpdateNewVersionAvailableTitle() {
        CoroutineScope(Dispatchers.Default).launch {
            val indicatorText = newVersionAvailableIndicator()
            CoroutineScope(Dispatchers.Main).launch {
                val titleText = strings[getTitleKey()] + "  " + indicatorText
                (activity as AppCompatActivity).supportActionBar?.title = titleText
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

    override fun onDestroyView() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        activity?.unregisterReceiver(receiver)
        super.onDestroyView()
    }

    companion object {
        private const val PROXIMITY_BUTTON_DELAY: Long = 2000L
        private const val DARK_STRING_KEY: String = "dark"
        private const val LIGHT_STRING_KEY: String = "light"
        private const val SHORT_LABEL_STRING_KEY: String = "shortLabel"
        private const val COLOR_CODE_STRING_KEY: String = "colorCode"
        private const val CURFEW_CERTIFICATE_SHORTCUT_ID: String = "curfewCertificateShortcut"
        private const val CURFEW_CERTIFICATE_SHORTCUT_URI: String = "tousanticovid://attestations/"
        const val START_PROXIMITY_ARG_KEY: String = "START_PROXIMITY_ARG_KEY"
    }
}
