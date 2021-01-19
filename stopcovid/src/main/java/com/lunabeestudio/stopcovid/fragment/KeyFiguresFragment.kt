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

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color.parseColor
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.core.view.MenuItemCompat
import androidx.core.view.isInvisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureCardBinding
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.colorStringKey
import com.lunabeestudio.stopcovid.extension.descriptionStringKey
import com.lunabeestudio.stopcovid.extension.formatNumberIfNeeded
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.getTrend
import com.lunabeestudio.stopcovid.extension.hasChosenPostalCode
import com.lunabeestudio.stopcovid.extension.labelStringKey
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showPostalCodeDialog
import com.lunabeestudio.stopcovid.fastitem.KeyFigureCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.keyFigureCardItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureCategory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class KeyFiguresFragment : MainFragment() {

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(robertManager.configuration.displayDepartmentLevel)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.postal_code_menu, menu)
        MenuItemCompat.setContentDescription(menu.findItem(R.id.item_map), strings["home.infoSection.newPostalCode.button"])
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        KeyFiguresManager.figures.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }

        binding?.emptyButton?.setOnClickListener {
            showLoading()
            viewLifecycleOwnerOrNull()?.lifecycleScope?.launch(Dispatchers.IO) {
                KeyFiguresManager.onAppForeground(requireContext())
                withContext(Dispatchers.Main) {
                    refreshScreen()
                }
            }
        }

        findNavControllerOrNull()?.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(
            PostalCodeBottomSheetFragment.SHOULD_BE_REFRESHED_KEY
        )?.observe(viewLifecycleOwner) { shouldBeRefreshed ->
            if (shouldBeRefreshed) {
                refreshScreen()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.item_map) {
            if (sharedPrefs.chosenPostalCode == null) {
                showPostalCodeDialog()
            } else {
                findNavControllerOrNull()?.safeNavigate(KeyFiguresFragmentDirections.actionKeyFiguresFragmentToPostalCodeBottomSheetFragment())
            }
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        KeyFiguresManager.figures.value?.peekContent()?.let { keyFigures ->
            if (keyFigures.isNotEmpty()) {
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }
                items += bigTitleItem {
                    text = strings["keyFiguresController.section.health"]
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["keyFiguresController.section.health.subtitle"]
                    identifier = text.hashCode().toLong()
                }
                items += linkItem {
                    text = strings["keyFiguresController.section.health.button"]
                    onClickListener = View.OnClickListener {
                        findNavControllerOrNull()?.safeNavigate(KeyFiguresFragmentDirections.actionKeyFiguresFragmentToMoreKeyFiguresFragment())
                    }
                    identifier = text.hashCode().toLong()
                }
                keyFigures.filter { it.category == KeyFigureCategory.HEALTH }.forEach { figure ->
                    items += itemForFigure(figure, false)
                }

                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }

                items += bigTitleItem {
                    text = strings["keyFiguresController.section.app"]
                    identifier = items.count().toLong()
                }
                keyFigures.filter { it.category == KeyFigureCategory.APP }.forEach { figure ->
                    items += itemForFigure(figure, true)
                }
            }
        }

        return items
    }

    override fun refreshScreen() {
        super.refreshScreen()

        binding?.emptyTitleTextView?.text = strings["infoCenterController.noInternet.title"]
        binding?.emptyDescriptionTextView?.text = strings["infoCenterController.noInternet.subtitle"]
        binding?.emptyButton?.text = strings["common.retry"]
    }

    @OptIn(ExperimentalTime::class)
    private fun itemForFigure(figure: KeyFigure, useDateTime: Boolean): KeyFigureCardItem {
        return keyFigureCardItem {
            val extractDate: Long
            if (sharedPrefs.hasChosenPostalCode) {
                val departmentKeyFigure = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)

                if (departmentKeyFigure != null) {
                    rightLocation = strings["common.country.france"]
                    leftLocation = departmentKeyFigure.dptLabel
                    leftValue = departmentKeyFigure.valueToDisplay?.formatNumberIfNeeded(numberFormat)
                    rightValue = figure.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
                    rightTrend = figure.trend?.getTrend()
                    leftTrend = departmentKeyFigure.trend?.getTrend()
                    extractDate = departmentKeyFigure.extractDate
                } else {
                    leftLocation = strings["common.country.france"]
                    leftValue = figure.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
                    leftTrend = figure.trend?.getTrend()
                    extractDate = figure.extractDate
                }
            } else {
                leftValue = figure.valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
                leftTrend = figure.trend?.getTrend()
                extractDate = figure.extractDate
            }
            updatedAt = stringsFormat(
                "keyFigures.update",
                if (useDateTime) {
                    extractDate.seconds.getRelativeDateTimeString(requireContext())
                } else {
                    extractDate.seconds.getRelativeDateString()
                }
            )

            label = strings[figure.labelStringKey]
            description = strings[figure.descriptionStringKey]
            identifier = figure.labelKey.hashCode().toLong()

            strings[figure.colorStringKey(requireContext().isNightMode())]?.let {
                color = parseColor(it)
            }

            shareContentDescription = strings["accessibility.hint.keyFigure.share"]
            onShareCard = { binding ->
                viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                    val uri = getShareCaptureUri(binding, "$label")
                    withContext(Dispatchers.Main) {
                        yield()
                        val shareString = if (rightLocation == null) {
                            stringsFormat("keyFigure.sharing.national", label, leftValue)
                        } else {
                            stringsFormat("keyFigure.sharing.department", label, leftLocation, leftValue, label, rightValue)
                        }
                        ShareManager.shareImageAndText(requireContext(), uri, shareString) {
                            strings["common.error.unknown"]?.let { showErrorSnackBar(it) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getShareCaptureUri(binding: ItemKeyFigureCardBinding, filenameWithoutExt: String): Uri? {
        val bitmap = getBitmapForItem(binding, requireActivity().window)
        return bitmap?.let {
            ShareManager.getShareCaptureUriFromBitmap(binding.root.context, it, filenameWithoutExt)
        }
    }

    private suspend fun getBitmapForItem(binding: ItemKeyFigureCardBinding, window: Window): Bitmap? {
        val view = binding.root
        binding.shareButton.isInvisible = true

        val finalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            try {
                suspendCoroutine<Bitmap> { continuation ->
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    PixelCopy.request(
                        window,
                        Rect(
                            locationOfViewInWindow[0],
                            locationOfViewInWindow[1],
                            locationOfViewInWindow[0] + view.width,
                            locationOfViewInWindow[1] + view.height
                        ), bitmap, { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            continuation.resume(bitmap)
                        } else {
                            continuation.resumeWithException(Exception("Failed to create bitmap"))
                        }
                    },
                        Handler(Looper.getMainLooper())
                    )
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        } else {
            view.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(view.drawingCache)
            view.isDrawingCacheEnabled = false
            bitmap
        }

        binding.shareButton.isInvisible = false

        return finalBitmap
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

    override fun getTitleKey(): String = "keyFiguresController.title"
}
