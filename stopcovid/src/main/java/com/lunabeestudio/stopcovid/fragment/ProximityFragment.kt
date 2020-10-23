/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
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
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
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
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.model.AtRiskStatus
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.utils.EventObserver
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.addRipple
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.fastitem.ImageBackgroundCardItem
import com.lunabeestudio.stopcovid.fastitem.InfoCenterCardItem
import com.lunabeestudio.stopcovid.fastitem.LogoItem
import com.lunabeestudio.stopcovid.fastitem.LottieItem
import com.lunabeestudio.stopcovid.fastitem.NumbersCardItem
import com.lunabeestudio.stopcovid.fastitem.ProximityButtonItem
import com.lunabeestudio.stopcovid.fastitem.State
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.coreui.fastitem.CaptionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.TitleItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.fastitem.buttonCardItem
import com.lunabeestudio.stopcovid.fastitem.imageBackgroundCardItem
import com.lunabeestudio.stopcovid.fastitem.imageCardItem
import com.lunabeestudio.stopcovid.fastitem.infoCenterCardItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.lottieItem
import com.lunabeestudio.stopcovid.fastitem.moreCardItem
import com.lunabeestudio.stopcovid.fastitem.numbersCardItem
import com.lunabeestudio.stopcovid.fastitem.proximityButtonItem
import com.lunabeestudio.stopcovid.manager.InfoCenterManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModel
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

class ProximityFragment : TimeMainFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: ProximityViewModel by viewModels {
        ProximityViewModelFactory(robertManager)
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private lateinit var lottieItem: LottieItem
    private lateinit var logoItem: LogoItem
    private lateinit var subTitleItem: TitleItem
    private lateinit var proximityButtonItem: ProximityButtonItem
    private lateinit var infoCenterCardItem: InfoCenterCardItem
    private lateinit var numbersCardItem: NumbersCardItem
    private var healthItem: ImageBackgroundCardItem? = null
    private var shouldRefresh: Boolean = false
    private var isProximityOn: Boolean = false
    private val interpolator = DecelerateInterpolator()
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                refreshItems()
            }
        }
    }
    private var proximityClickThreshold = 0L

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
        return "proximityController.tabBar.title"
    }

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Constants.SharedPrefs.HAS_NEWS) {
            refreshItems()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        activity?.registerReceiver(receiver, filter)

        getActivityBinding()?.errorLayout?.let { errorLayout ->
            errorLayout.post {
                errorLayout.translationY = errorLayout.height.toFloat()
            }
        }

        isProximityOn = ProximityManager.isProximityOn(requireContext(), robertManager)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
        initHasNewsObserver()
        shouldRefresh = false
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
        }
        robertManager.atRiskStatus.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) { isAtRisk ->
            when {
                isAtRisk == AtRiskStatus.UNKNOWN -> {
                    healthItem = null
                    refreshScreen()
                }
                healthItem == null -> refreshScreen()
                isAtRisk == AtRiskStatus.AT_RISK -> refreshHealthItemAsync(true)
                isAtRisk == AtRiskStatus.NOT_AT_RISK -> refreshHealthItemAsync(false)
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

        addTopImageItems(items)
        addSubTitleItem(items)
        addActivateButtonItems(items)
        healthItem?.let { items += it } ?: addHealthItems(items)
        addNewsItems(items)
        addDeclareItems(items)
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
        lottieItem = lottieItem {
            identifier = items.count().toLong()
        }
        logoItem = logoItem {
            identifier = items.count().toLong()
        }
        if (isAnimationEnabled()) {
            items += lottieItem
        } else {
            items += logoItem
        }
    }

    private fun addActivateButtonItems(items: ArrayList<GenericItem>) {
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
            items += proximityButtonItem
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

    private fun addHealthItems(items: ArrayList<GenericItem>) {
        robertManager.isAtRisk?.let { atRisk ->
            healthItem = imageBackgroundCardItem {
                iconRes = R.drawable.health_card
                isAtRisk = atRisk
                onClickListener = View.OnClickListener {
                    findNavController().navigate(ProximityFragmentDirections.actionProximityFragmentToHealthFragment())
                }
                identifier = items.count().toLong()
            }

            refreshHealthItemAsync(atRisk)

            healthItem?.let { items += it }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
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
                findNavController().navigate(ProximityFragmentDirections.actionProximityFragmentToKeyFiguresFragment())
            }
            identifier = header.hashCode().toLong()
        }
        items += numbersCardItem

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        infoCenterCardItem = infoCenterCardItem {
            header = strings["home.infoSection.lastInfo"]
            link = strings["home.infoSection.readAll"]
            contentDescription = strings["home.infoSection.readAll"]
            showBadge = sharedPrefs.getBoolean(Constants.SharedPrefs.HAS_NEWS, false)
            onClickListener = View.OnClickListener {
                findNavController().navigate(ProximityFragmentDirections.actionProximityFragmentToInfoCenterFragment())
            }
            identifier = header.hashCode().toLong()
        }
        items += infoCenterCardItem

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
    }

    private fun addDeclareItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.declareSection.title"]
            identifier = items.count().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += imageCardItem {
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

    private fun addSharingItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["home.sharingSection.title"]
            identifier = items.count().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += imageCardItem {
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

            testText = strings["home.moreSection.testingSites"]
            testOnClickListener = View.OnClickListener {
                strings["myHealthController.testingSites.url"]?.openInExternalBrowser(requireContext())
            }
            testIconRes = R.drawable.ic_search

            documentText = strings["home.moreSection.curfewCertificate"]
            documentOnClickListener = View.OnClickListener {
                strings["home.moreSection.curfewCertificate.url"]?.openInExternalBrowser(requireContext())
            }
            documentIconRes = R.drawable.ic_document

            privacyText = strings["home.moreSection.privacy"]
            privacyOnClickListener = View.OnClickListener {
                findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToPrivacyFragment())
            }
            privacyIconRes = R.drawable.ic_privacy
            manageDataText = strings["home.moreSection.manageData"]
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
        if (!robertManager.isRegistered) {
            viewModel.refreshConfig(requireContext().applicationContext as RobertApplication)
        } else {
            bindToProximityService()
            viewModel.activateProximity(requireContext().applicationContext as RobertApplication)
        }
    }

    private fun deactivateProximity() {
        robertManager.deactivateProximity(requireContext().applicationContext as RobertApplication)

        view?.rootView?.announceForAccessibility(strings["notification.proximityServiceNotRunning.title"])
    }

    @OptIn(ExperimentalTime::class)
    @SuppressLint("RestrictedApi")
    private fun refreshItems() {
        context?.let { context ->
            val freshProximityOn = ProximityManager.isProximityOn(context, robertManager)
            val wasProximityDifferent: Boolean = isProximityOn != freshProximityOn
            isProximityOn = freshProximityOn
            lottieItem.state = if (wasProximityDifferent) {
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
            logoItem.imageRes = if (isProximityOn) {
                R.drawable.status_active
            } else {
                R.drawable.status_inactive
            }

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

                        keyFigures[0].let { (_, labelKey, valueGlobalToDisplay) ->
                            label1 = strings["${labelKey}.$SHORT_LABEL_STRING_KEY"]
                            value1 = valueGlobalToDisplay
                            color1 = strings["${labelKey}.$COLOR_CODE_STRING_KEY.$mode"]?.let { Color.parseColor(it) }
                        }

                        keyFigures[1].let { (_, labelKey, valueGlobalToDisplay) ->
                            label2 = strings["${labelKey}.$SHORT_LABEL_STRING_KEY"]
                            value2 = valueGlobalToDisplay
                            color2 = strings["${labelKey}.$COLOR_CODE_STRING_KEY.$mode"]?.let { Color.parseColor(it) }
                        }

                        keyFigures[2].let { (_, labelKey, valueGlobalToDisplay) ->
                            label3 = strings["${labelKey}.$SHORT_LABEL_STRING_KEY"]
                            value3 = valueGlobalToDisplay
                            color3 = strings["${labelKey}.$COLOR_CODE_STRING_KEY.$mode"]?.let { Color.parseColor(it) }
                        }
                    }
                }

            proximityButtonItem.showMainButton = !isProximityOn
            proximityButtonItem.isButtonEnabled = ProximityManager.isPhoneSetup(context)

            if (isAdded && !isHidden) {
                (activity as AppCompatActivity).supportActionBar?.title = strings[getTitleKey()]
                updateErrorLayout(getActivityBinding()?.errorLayout, freshProximityOn)
            }

            subTitleItem.text = if (isProximityOn) {
                strings["proximityController.switch.subtitle.activated"] + "\n" +
                strings["accessibility.hint.proximity.buttonState.activated"]
            } else {
                strings["proximityController.switch.subtitle.deactivated"] + "\n" +
                strings["accessibility.hint.proximity.buttonState.deactivated"]
            }

            if (binding?.recyclerView?.isComputingLayout == false) {
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun setTitle() {
        // Handled in refresh item
    }

    @OptIn(ExperimentalTime::class)
    private fun refreshHealthItemAsync(isAtRisk: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            healthItem?.apply {
                header = stringsFormat(
                    "myHealthController.notification.update",
                    robertManager.atRiskLastRefresh?.milliseconds?.getRelativeDateTimeString(requireContext())
                )

                if (isAtRisk) {
                    title = strings["home.healthSection.contact.cellTitle"]
                    subtitle = strings["home.healthSection.contact.cellSubtitle"]
                } else {
                    title = strings["home.healthSection.noContact.cellTitle"]
                    subtitle = strings["home.healthSection.noContact.cellSubtitle"]
                }

                this.isAtRisk = isAtRisk
            }

            if (binding?.recyclerView?.isComputingLayout == false) {
                withContext(Dispatchers.Main) {
                    binding?.recyclerView?.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun timeRefresh() {
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
                    binding?.recyclerView?.adapter?.notifyDataSetChanged()
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
        } else {
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
        super.onDestroyView()
    }

    companion object {
        private const val PROXIMITY_BUTTON_DELAY: Long = 2000L
        private const val DARK_STRING_KEY: String = "dark"
        private const val LIGHT_STRING_KEY: String = "light"
        private const val SHORT_LABEL_STRING_KEY: String = "shortLabel"
        private const val COLOR_CODE_STRING_KEY: String = "colorCode"
    }
}
