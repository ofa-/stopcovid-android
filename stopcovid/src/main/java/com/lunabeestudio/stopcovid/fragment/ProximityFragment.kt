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
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.addRipple
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.fastitem.CaptionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.TitleItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.fastitem.LogoItem
import com.lunabeestudio.stopcovid.fastitem.ProximityButtonItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.proximityButtonItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModel
import com.lunabeestudio.stopcovid.viewmodel.ProximityViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

class ProximityFragment : AboutMainFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: ProximityViewModel by viewModels {
        ProximityViewModelFactory(robertManager)
    }

    private lateinit var logoItem: LogoItem
    private lateinit var proximityButtonItem: ProximityButtonItem
    private lateinit var captionItem: CaptionItem
    private lateinit var subTitleItem: TitleItem
    private var webView: WebView? = null
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
        return if (robertManager.isProximityActive) {
            "common.bravo"
        } else {
            "common.warning"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        activity?.registerReceiver(receiver, filter)

        getActivityBinding().errorLayout.post {
            getActivityBinding().errorLayout.translationY = getActivityBinding().errorLayout.height.toFloat()
        }
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
        refreshScreen()
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
        viewModel.activateProximitySuccess.observe(viewLifecycleOwner) {
            refreshItems()
        }
    }

    private fun bindToProximityService() {
        requireActivity().bindService(ProximityService.intent(requireActivity()), proximityServiceConnection, Context.BIND_ABOVE_CLIENT)
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unbindService(proximityServiceConnection)
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        subTitleItem = titleItem {
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += subTitleItem

        logoItem = logoItem {
            identifier = items.count().toLong()
        }
        items += logoItem
        proximityButtonItem = proximityButtonItem {
            mainText = strings["proximityController.button.activateProximity"]
            lightText = strings["proximityController.button.deactivateProximity"]
            onClickListener = View.OnClickListener {
                if (System.currentTimeMillis() > proximityClickThreshold) {
                    proximityClickThreshold = System.currentTimeMillis() + PROXIMITY_BUTTON_DELAY
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
                findNavController().navigate(ProximityFragmentDirections.actionProximityFragmentToPrivacyFragment())
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
                findNavController().navigate(ProximityFragmentDirections.actionProximityFragmentToManageDataFragment())
            }
            iconRes = R.drawable.ic_manage_data
            identifier = items.count().toLong()
        }

        refreshItems()

        return items
    }

    private fun activateProximity() {
        if (!robertManager.isRegistered) {
            injectWebView()
        } else {
            bindToProximityService()
            viewModel.activateProximity(requireContext().applicationContext as RobertApplication)
        }
    }

    private fun refreshItems() {
        context?.let { context ->
            val isProximityOn: Boolean = ProximityManager.isProximityOn(context, robertManager)
            logoItem.imageRes = if (isProximityOn) {
                R.drawable.status_active
            } else {
                R.drawable.status_inactive
            }

            proximityButtonItem.showMainButton = !isProximityOn
            proximityButtonItem.isButtonEnabled = ProximityManager.isPhoneSetup(context)

            captionItem.text = if (isProximityOn) {
                strings["proximityController.mainMessage.subtitle.on"]
            } else {
                strings["proximityController.mainMessage.subtitle.off"]
            }

            subTitleItem.text = if (isProximityOn) {
                strings["proximityController.switch.subtitle.activated"]
            } else {
                strings["proximityController.switch.subtitle.deactivated"]
            }

            updateErrorLayout(getActivityBinding().errorLayout)

            if (binding?.recyclerView?.isComputingLayout == false) {
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun updateErrorLayout(errorLayout: FrameLayout) {
        getActivityBinding().errorTextView.text = ProximityManager.getErrorText(this, robertManager, strings)
        val clickListener = ProximityManager.getErrorClickListener(this) {
            if (System.currentTimeMillis() > proximityClickThreshold) {
                proximityClickThreshold = System.currentTimeMillis() + PROXIMITY_BUTTON_DELAY
                activateProximity()
            }
        }
        getActivityBinding().errorTextView.setOnClickListener(clickListener)
        if (clickListener != null) {
            getActivityBinding().errorTextView.addRipple()
        } else {
            getActivityBinding().errorTextView.background = null
        }
        if (webView?.isVisible == true || ProximityManager.isProximityOn(requireContext(), robertManager)) {
            hideErrorLayout(errorLayout)
        } else {
            showErrorLayout(errorLayout)
        }
    }

    private fun hideErrorLayout(errorLayout: FrameLayout) {
        errorLayout.errorTextView.isClickable = false
        errorLayout.animate().apply {
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

    private fun showErrorLayout(errorLayout: FrameLayout) {
        errorLayout.post {
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

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun injectWebView() {
        (activity as? MainActivity)?.showProgress(true)
        webView = WebView(requireContext())
        webView?.settings?.javaScriptEnabled = true
        webView?.settings?.allowFileAccessFromFileURLs = false
        webView?.settings?.allowUniversalAccessFromFileURLs = false
        webView?.settings?.allowContentAccess = false
        webView?.settings?.allowFileAccess = false
        webView?.settings?.setGeolocationEnabled(false)
        webView?.settings?.javaScriptCanOpenWindowsAutomatically = false
        webView?.settings?.saveFormData = false
        webView?.settings?.savePassword = false
        webView?.setBackgroundColor(R.attr.colorSurface.fetchSystemColor(requireContext()))
        webView?.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return try {
                    // Let loadDataWithBaseURL intial data load and network-security-config compatible API pass
                    if (request.url.toString() == "data:text/html;charset=utf-8;base64,") {
                        //                        || Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                        super.shouldInterceptRequest(view, request)
                    } else {
                        // validate Trust anchor
                        val url = URL(request.url.toString())
                        var response = RetrofitClient.getDefaultOKHttpClient(requireContext(),
                            com.lunabeestudio.framework.BuildConfig.BASE_URL,
                            com.lunabeestudio.framework.BuildConfig.CERTIFICATE_SHA256)
                            .newCall(Request.Builder()
                                .url(url)
                                .build()).execute()
                        if (response.isSuccessful) {
                            // Success
                            super.shouldInterceptRequest(view, request)
                        } else {
                            response = RetrofitClient.getDefaultOKHttpClient(requireContext(),
                                com.lunabeestudio.framework.BuildConfig.BASE_URL,
                                com.lunabeestudio.framework.BuildConfig.CERTIFICATE_SHA256)
                                .newCall(Request.Builder()
                                    .url(url)
                                    .post("".toRequestBody())
                                    .build()).execute()
                            if (response.isSuccessful) {
                                // Success
                                super.shouldInterceptRequest(view, request)
                            } else {
                                // Fail
                                WebResourceResponse(null, null, null)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    // Fail
                    WebResourceResponse(null, null, null)
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                (activity as? MainActivity)?.showProgress(false)
                (activity as? MainActivity)?.showErrorSnackBar(strings["common.error.internet"] ?: "")
                removeWebView()
                refreshItems()
            }
        }
        webView?.addJavascriptInterface(object {
            @JavascriptInterface
            fun token(message: String) {
                lifecycleScope.launch {
                    removeWebView()
                    context?.let {
                        bindToProximityService()
                        viewModel.register(requireContext().applicationContext as RobertApplication, message)
                    }
                }
            }

            @JavascriptInterface
            fun didLoad() {
                lifecycleScope.launch {
                    webView?.evaluateJavascript("execute();", null)
                }
            }

            @JavascriptInterface
            fun showReCaptcha() {
                lifecycleScope.launch {
                    (activity as? MainActivity)?.showProgress(false)
                    webView?.isVisible = true
                    refreshItems()
                }
            }

            @JavascriptInterface
            fun error() {
                lifecycleScope.launch {
                    (activity as? MainActivity)?.showProgress(false)
                    (activity as? MainActivity)?.showErrorSnackBar(strings["proximityService.error.captchaError"] ?: "")
                    removeWebView()
                    refreshItems()
                }
            }
        }, JAVASCRIPT_INTERFACE_NAME)
        val script = getString(R.string.captcha_script,
            BuildConfig.CAPTCHA_API_KEY,
            JAVASCRIPT_INTERFACE_NAME)
        val insideScript = script.split("<script type=\"text/javascript\">")[1].split("</script>")[0]
        val insideScript256 = MessageDigest.getInstance("SHA-256").digest(insideScript.toByteArray())
        val scriptSha256 = Base64.encodeToString(insideScript256, Base64.NO_WRAP)
        val html = getString(R.string.captcha_html,
            script,
            scriptSha256,
            Locale.getDefault().language)
        webView?.loadDataWithBaseURL(BuildConfig.CAPTCHA_URL,
            html,
            "text/html",
            "utf-8",
            null)
        webView?.isInvisible = true
        (binding?.root as? ViewGroup)?.addView(webView)
        refreshItems()
    }

    private fun removeWebView() {
        webView?.settings?.javaScriptEnabled = false
        webView?.webViewClient = null
        webView?.removeJavascriptInterface(JAVASCRIPT_INTERFACE_NAME)
        (binding?.root as? ViewGroup)?.removeView(webView)
        webView = null
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
        private const val JAVASCRIPT_INTERFACE_NAME: String = "androidJSHandler"
        private const val PROXIMITY_BUTTON_DELAY: Long = 1000L
    }
}
