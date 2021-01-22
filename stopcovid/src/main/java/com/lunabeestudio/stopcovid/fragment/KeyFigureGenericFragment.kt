package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureCardBinding
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.showPostalCodeDialog
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class KeyFigureGenericFragment : MainFragment() {

    protected val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    protected val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(robertManager.configuration.displayDepartmentLevel)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.postal_code_menu, menu)
        MenuItemCompat.setContentDescription(menu.findItem(R.id.item_map), strings["home.infoSection.newPostalCode.button"])
    }

    override fun refreshScreen() {
        super.refreshScreen()

        binding?.emptyTitleTextView?.text = strings["infoCenterController.noInternet.title"]
        binding?.emptyDescriptionTextView?.text = strings["infoCenterController.noInternet.subtitle"]
        binding?.emptyButton?.text = strings["common.retry"]
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

    abstract fun showPostalCodeBottomSheet()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.item_map) {
            if (sharedPrefs.chosenPostalCode == null) {
                showPostalCodeDialog()
            } else {
                showPostalCodeBottomSheet()
            }
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    protected suspend fun getShareCaptureUri(binding: ItemKeyFigureCardBinding, filenameWithoutExt: String): Uri {
        val bitmap = getBitmapForItem(binding)
        return ShareManager.getShareCaptureUriFromBitmap(binding.root.context, bitmap, filenameWithoutExt)
    }

    private suspend fun getBitmapForItem(binding: ItemKeyFigureCardBinding): Bitmap {
        val savedShareVisibility = binding.shareButton.isVisible
        binding.shareButton.isVisible = false
        val savedDescriptionVisibility = binding.descriptionTextView.isVisible
        binding.descriptionTextView.isVisible = false
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(binding.root.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )

        val bitmap = Bitmap.createBitmap(binding.root.measuredWidth, binding.root.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        suspendCoroutine<Unit> { continuation ->
            binding.root.post {
                binding.root.draw(canvas)
                continuation.resume(Unit)
            }
        }

        binding.shareButton.isVisible = savedShareVisibility
        binding.descriptionTextView.isVisible = savedDescriptionVisibility
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(binding.root.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )

        return bitmap
    }

}