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
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.utils.Utils
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.addRipple
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.fastitem.CaptionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.TitleItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.fastitem.LogoItem
import com.lunabeestudio.stopcovid.fastitem.LottieItem
import com.lunabeestudio.stopcovid.fastitem.ProximityButtonItem
import com.lunabeestudio.stopcovid.fastitem.State
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.lottieItem
import com.lunabeestudio.stopcovid.fastitem.proximityButtonItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModel
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.android.synthetic.main.activity_main.view.*

class ProximityFragment : AboutMainFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: ProximityViewModel by viewModels {
        ProximityViewModelFactory(robertManager)
    }

    private lateinit var lottieItem: LottieItem
    private lateinit var logoItem: LogoItem
    private lateinit var proximityButtonItem: ProximityButtonItem
    private lateinit var captionItem: CaptionItem
    private lateinit var subTitleItem: TitleItem
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

        subTitleItem = titleItem {
            onClick = {
                findNavController().navigate(ProximityFragmentDirections.actionProximityFragmentToTuneProximityFragment())
            }
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }

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
        items += subTitleItem

        proximityButtonItem = proximityButtonItem {
            mainText = strings["proximityController.button.activateProximity"]
            lightText = strings["proximityController.button.deactivateProximity"]
            onClickListener = View.OnClickListener {
                if (SystemClock.elapsedRealtime() > proximityClickThreshold) {
                    proximityClickThreshold = SystemClock.elapsedRealtime() + PROXIMITY_BUTTON_DELAY
                    if (robertManager.isProximityActive) {
                        robertManager.deactivateProximity(requireContext().applicationContext as RobertApplication)
                        refreshItems()
                    } else {
                        activateProximity()
                    }
                }
            }
            identifier = items.count().toLong()
        }
        items += proximityButtonItem
        captionItem = captionItem {
            text = if (robertManager.isProximityActive) {
                strings["proximityController.mainMessage.subtitle.on"]
            } else {
                strings["proximityController.mainMessage.subtitle.off"]
            }
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += dividerItem {
            identifier = items.count().toLong()
        }

        items += linkItem {
            text = strings["privacyController.tabBar.title"]
            onClickListener = View.OnClickListener {
                findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToPrivacyFragment())
            }
            iconRes = R.drawable.ic_privacy
            identifier = items.count().toLong()
        }

        items += dividerItem {
            identifier = items.count().toLong()
        }

        items += linkItem {
            text = strings["proximityController.manageData"]
            onClickListener = View.OnClickListener {
                findNavController().safeNavigate(ProximityFragmentDirections.actionProximityFragmentToManageDataFragment())
            }
            iconRes = R.drawable.ic_manage_data
            identifier = items.count().toLong()
        }

        items += dividerItem {
            identifier = items.count().toLong()
        }

        items += linkItem {
            text = strings["proximityController.tuneProximity"]
            onClickListener = View.OnClickListener {
                findNavController().navigate(ProximityFragmentDirections.actionProximityFragmentToTuneProximityFragment())
            }
            iconRes = R.drawable.ic_manage_data
            identifier = items.count().toLong()
        }

        refreshItems()

        return items
    }

    private fun activateProximity() {
        if (!robertManager.isRegistered) {
            viewModel.refreshConfig(requireContext().applicationContext as RobertApplication)
        } else {
            bindToProximityService()
            viewModel.activateProximity(requireContext().applicationContext as RobertApplication)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun refreshItems() {
        context?.let { context ->
            val wasProximityDifferent: Boolean = isProximityOn != ProximityManager.isProximityOn(context, robertManager)
            isProximityOn = ProximityManager.isProximityOn(context, robertManager)
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
            (activity as AppCompatActivity).supportActionBar?.title = strings[getTitleKey()]

            proximityButtonItem.showMainButton = !isProximityOn
            proximityButtonItem.isButtonEnabled = ProximityManager.isPhoneSetup(context)

            captionItem.text = if (isProximityOn) {
                strings["proximityController.mainMessage.subtitle.on"]
            } else {
                strings["proximityController.mainMessage.subtitle.off"]
            }

            subTitleItem.text = if (isProximityOn) {
                strings["proximityController.switch.subtitle.activated"] + "\n" +
                strings["accessibility.hint.proximity.buttonState.activated"]
            } else {
                strings["proximityController.switch.subtitle.deactivated"] + "\n" +
                strings["accessibility.hint.proximity.buttonState.deactivated"]
            }

            updateErrorLayout(getActivityBinding()?.errorLayout)

            if (binding?.recyclerView?.isComputingLayout == false) {
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun isAnimationEnabled(): Boolean = Utils.getAnimationScale(getContext()) != 0f

    private fun updateErrorLayout(errorLayout: FrameLayout?) {
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
        if (ProximityManager.isProximityOn(requireContext(), robertManager)) {
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
        activity?.unregisterReceiver(receiver)
        super.onDestroyView()
    }

    companion object {
        private const val PROXIMITY_BUTTON_DELAY: Long = 2000L
    }
}
