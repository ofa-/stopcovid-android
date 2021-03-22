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
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.gson.Gson
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.databinding.ActivityAppMaintenanceBinding
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.manager.AppMaintenanceManager
import com.lunabeestudio.stopcovid.model.Info
import kotlinx.coroutines.launch

/**
 * The blocking activity
 */
class AppMaintenanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppMaintenanceBinding

    private var strings: LocalizedStrings = StringsManager.strings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAppMaintenanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val info = Gson().fromJson(intent.getStringExtra(EXTRA_INFO), Info::class.java)
        fillScreen(info)
        bindProgressButton(binding.refreshButton)
    }

    /**
     * Fill the screen with
     * @param info information
     */
    private fun fillScreen(info: Info) {
        if (info.message != null) {
            binding.textView.text = info.message
            binding.textView.visibility = View.VISIBLE
        } else {
            binding.textView.visibility = View.GONE
        }
        if (info.buttonTitle != null && info.buttonUrl != null) {
            binding.button.text = info.buttonTitle
            binding.button.setOnClickListener {
                startOpenInStore()
            }
            binding.button.visibility = View.VISIBLE
        } else {
            binding.button.visibility = View.GONE
        }
        when (info.mode) {
            Info.Mode.MAINTENANCE -> {
                binding.imageView.setImageResource(AppMaintenanceManager.maintenanceIconRes)
                binding.imageView.visibility = View.VISIBLE
            }
            Info.Mode.UPGRADE -> {
                binding.imageView.setImageResource(AppMaintenanceManager.upgradeIconRes)
                binding.imageView.visibility = View.VISIBLE
            }
            else -> {
                binding.imageView.visibility = View.GONE
            }
        }
        binding.refreshButton.isVisible = info.mode == Info.Mode.MAINTENANCE
        binding.refreshButton.text = strings["common.tryAgain"]
        binding.refreshButton.setOnClickListener {
            binding.refreshButton.showProgress {
                progressColor = ContextCompat.getColor(this@AppMaintenanceActivity, R.color.color_on_primary)
                gravity = DrawableButton.GRAVITY_CENTER
            }
            lifecycleScope.launch {
                AppMaintenanceManager.updateCheckForMaintenanceUpgrade(this@AppMaintenanceActivity,
                    appIsFreeCompletion = {
                        startActivity(Intent(this@AppMaintenanceActivity, OnBoardingActivity::class.java))
                        finishAndRemoveTask()

                    },
                    appIsBlockedCompletion = { info ->
                        binding.refreshButton.hideProgress(strings["common.tryAgain"])
                        binding.swipeRefreshLayout.isRefreshing = false
                        fillScreen(info)
                    })
            }
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                AppMaintenanceManager.updateCheckForMaintenanceUpgrade(this@AppMaintenanceActivity,
                    appIsFreeCompletion = {
                        startActivity(Intent(this@AppMaintenanceActivity, OnBoardingActivity::class.java))
                        finishAndRemoveTask()

                    },
                    appIsBlockedCompletion = { info ->
                        binding.swipeRefreshLayout.isRefreshing = false
                        fillScreen(info)
                    })
            }
        }
    }

    private fun startOpenInStore() {
        if (!ConfigConstant.Store.GOOGLE.openInExternalBrowser(this, false)) {
            if (!ConfigConstant.Store.HUAWEI.openInExternalBrowser(this, false)) {
                ConfigConstant.Store.WEBSITE.openInExternalBrowser(this)
            }
        }
    }

    /**
     * If user try to quitStopCovid this screen, it quitStopCovid the app.
     * This activity is blocking the app
     */
    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    companion object {
        const val EXTRA_INFO: String = "extra.info"
    }
}
