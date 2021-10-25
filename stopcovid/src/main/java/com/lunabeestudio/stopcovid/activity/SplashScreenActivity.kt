package com.lunabeestudio.stopcovid.activity

import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.liveStrings
import com.lunabeestudio.stopcovid.coreui.extension.strings
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.databinding.ActivitySplashScreenBinding
import com.lunabeestudio.stopcovid.extension.isOnBoardingDone
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class SplashScreenActivity : BaseActivity() {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    var splashLoadingJob: Job? = null
    var noStringDialog: AlertDialog? = null
    lateinit var splashScreenBinding: ActivitySplashScreenBinding

    @OptIn(ExperimentalTime::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        splashScreenBinding = ActivitySplashScreenBinding.inflate(layoutInflater)

        // Wait 2 + 5 seconds to load strings from file or server. Show blocking error if we still don't have strings.
        splashLoadingJob = lifecycleScope.launchWhenResumed {
            delay(Duration.seconds(2))
            splashScreenBinding.progressBar.show()
            delay(Duration.seconds(5))
            splashScreenBinding.progressBar.hide()
            showNoStringsErrorDialog()
        }

        setContentView(splashScreenBinding.root)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isNightMode()

        if (strings.isEmpty()) {
            val stringsObserver = object : Observer<Event<LocalizedStrings>> {
                override fun onChanged(strings: Event<LocalizedStrings>?) {
                    if (!strings?.peekContent().isNullOrEmpty()) {
                        liveStrings.removeObserver(this)
                        startOnBoardingOrMain(sharedPreferences.isOnBoardingDone)
                    }
                }
            }
            liveStrings.observe(this@SplashScreenActivity, stringsObserver)
        } else {
            startOnBoardingOrMain(sharedPreferences.isOnBoardingDone)
        }
    }

    private fun startOnBoardingOrMain(onBoardingDone: Boolean) {

        splashLoadingJob?.cancel("Starting on boarding")
        splashScreenBinding.progressBar.hide()
        noStringDialog?.dismiss()

        if (onBoardingDone) {
            val intent = Intent(this@SplashScreenActivity, MainActivity::class.java).apply {
                data = intent.data
                addFlags(intent.flags)
            }
            startActivity(
                intent,
                ActivityOptions
                    .makeCustomAnimation(
                        this@SplashScreenActivity,
                        R.anim.nav_default_enter_anim,
                        R.anim.nav_default_exit_anim
                    )
                    .toBundle()
            )
            finishAndRemoveTask()
        } else {
            val intent = Intent(this@SplashScreenActivity, OnBoardingActivity::class.java).apply {
                data = intent.data
            }
            startActivity(
                intent,
                ActivityOptions
                    .makeCustomAnimation(
                        this@SplashScreenActivity,
                        R.anim.nav_default_enter_anim,
                        R.anim.nav_default_exit_anim
                    )
                    .toBundle()
            )
            finishAndRemoveTask()
        }
    }

    private fun showNoStringsErrorDialog() {
        if (!isFinishing) {
            if (strings.isEmpty()) {
                noStringDialog = MaterialAlertDialogBuilder(this@SplashScreenActivity)
                    .setTitle(getString(R.string.splashScreenErrorDialog_title, getString(R.string.app_name)))
                    .setMessage(R.string.splashScreenErrorDialog_message)
                    .setPositiveButton(R.string.splashScreenErrorDialog_positiveButton) { _, _ ->
                        splashLoadingJob = lifecycleScope.launch {
                            splashScreenBinding.progressBar.show()
                            (application as? StopCovid)?.injectionContainer?.stringsManager?.let { stringsManager ->
                                stringsManager.initialize(this@SplashScreenActivity)
                                stringsManager.onAppForeground(this@SplashScreenActivity)
                            }
                            splashScreenBinding.progressBar.hide()
                            showNoStringsErrorDialog()
                        }
                    }
                    .setNegativeButton(R.string.splashScreenErrorDialog_negativeButton) { _, _ ->
                        exitProcess(0)
                    }
                    .setOnDismissListener {
                        noStringDialog = null
                    }
                    .show()
            } else {
                startOnBoardingOrMain(sharedPreferences.isOnBoardingDone)
            }
        }
    }
}