package com.lunabeestudio.stopcovid.activity

import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.databinding.ActivitySplashScreenBinding
import com.lunabeestudio.stopcovid.extension.isOnBoardingDone
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

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
        val onBoardingDone = sharedPreferences.isOnBoardingDone

        splashScreenBinding = ActivitySplashScreenBinding.inflate(layoutInflater)

        // Wait 2 + 5 seconds to load strings from file or server. Show blocking error if we still don't have strings.
        splashLoadingJob = lifecycleScope.launchWhenResumed {
            delay(2.seconds)
            splashScreenBinding.progressBar.show()
            delay(5.seconds)
            splashScreenBinding.progressBar.hide()
            showNoStringsErrorDialog()
        }

        setContentView(splashScreenBinding.root)

        if (StringsManager.strings.isEmpty()) {
            val stringsObserver = object : Observer<Event<HashMap<String, String>>> {
                override fun onChanged(strings: Event<HashMap<String, String>>?) {
                    if (!strings?.peekContent().isNullOrEmpty()) {
                        StringsManager.liveStrings.removeObserver(this)
                        startOnBoardingOrMain(onBoardingDone)
                    }
                }
            }
            StringsManager.liveStrings.observe(this@SplashScreenActivity, stringsObserver)
        } else {
            startOnBoardingOrMain(onBoardingDone)
        }
    }

    private fun startOnBoardingOrMain(onBoardingDone: Boolean) {

        splashLoadingJob?.cancel("Starting on boarding")
        splashScreenBinding.progressBar.hide()
        noStringDialog?.dismiss()

        if (onBoardingDone) {
            val intent = Intent(this@SplashScreenActivity, MainActivity::class.java).apply {
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
            noStringDialog = MaterialAlertDialogBuilder(this@SplashScreenActivity)
                .setTitle(getString(R.string.splashScreenErrorDialog_title, getString(R.string.app_name)))
                .setMessage(R.string.splashScreenErrorDialog_message)
                .setPositiveButton(R.string.splashScreenErrorDialog_positiveButton) { _, _ ->
                    splashLoadingJob = lifecycleScope.launch {
                        splashScreenBinding.progressBar.show()
                        StringsManager.initialize(this@SplashScreenActivity)
                        StringsManager.onAppForeground(this@SplashScreenActivity)
                        splashScreenBinding.progressBar.hide()
                        if (StringsManager.strings.isEmpty()) {
                            showNoStringsErrorDialog()
                        }
                    }
                }
                .setNegativeButton(R.string.splashScreenErrorDialog_negativeButton) { _, _ ->
                    exitProcess(0)
                }
                .setOnDismissListener {
                    noStringDialog = null
                }
                .show()
        }
    }
}