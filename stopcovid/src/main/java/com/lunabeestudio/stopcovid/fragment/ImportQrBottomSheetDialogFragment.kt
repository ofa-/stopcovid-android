/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/30/09 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.Result
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.ImportQRCodeActivity
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fragment.FastAdapterBottomSheetDialogFragment
import com.lunabeestudio.stopcovid.extension.showPasswordDialog
import com.lunabeestudio.stopcovid.fastitem.selectionItem
import com.lunabeestudio.stopcovid.viewmodel.ImportQrBottomViewModel
import com.lunabeestudio.stopcovid.viewmodel.ImportQrBottomViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class ImportQrBottomSheetDialogFragment : FastAdapterBottomSheetDialogFragment() {

    private val pickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { activityResult -> onPickerResult(activityResult) }

    private val viewModel: ImportQrBottomViewModel by viewModels {
        ImportQrBottomViewModelFactory(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.scanResult.observe(viewLifecycleOwner) {
            onScanResult(it)
        }
        viewModel.passwordFailure.observe(viewLifecycleOwner) { inError ->
            showPasswordDialog(inError)
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            (activity as? ImportQRCodeActivity)?.showProgress(loading)
        }
    }

    override fun refreshScreen() {
        val items = arrayListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["universalQrScanController.actionSheet.title"]
            identifier = "universalQrScanController.actionSheet.title".hashCode().toLong()
        }
        items += selectionItem {
            title = strings["universalQrScanController.actionSheet.imagePicker"]
            showSelection = false
            onClick = ::openGallery
            identifier = "universalQrScanController.actionSheet.imagePicker".hashCode().toLong()
        }
        items += selectionItem {
            title = strings["universalQrScanController.actionSheet.documentPicker"]
            showSelection = false
            onClick = ::openPdfFile
            identifier = "universalQrScanController.actionSheet.documentPicker".hashCode().toLong()
        }

        adapter.setNewList(items)
    }

    private fun openGallery() {
        val intent = Intent()
            .setType("image/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        pickerLauncher.launch(intent)
    }

    private fun openPdfFile() {
        val intent = Intent()
            .setType("application/pdf")
            .setAction(Intent.ACTION_GET_CONTENT)

        pickerLauncher.launch(intent)
    }

    private fun onPickerResult(activityResult: ActivityResult) {
        context?.let { context ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                val uri = activityResult.data?.data

                val mimeType: String? = uri?.let { context.contentResolver?.getType(it) }

                if (mimeType == null || mimeType.startsWith("image/")) {
                    viewModel.scanImageFile(context, uri)
                } else if (mimeType == "application/pdf") {
                    viewModel.scanPdfFile(context, uri)
                }
            }
        }
    }

    fun onScanResult(result: Result?) {
        if (result != null) {
            val intent = Intent()
            intent.putExtra(
                ImportQRCodeActivity.EXTRA_CODE_SCANNED,
                result.text
            )
            activity?.setResult(Activity.RESULT_OK, intent)
        } else {
            activity?.setResult(ImportQRCodeActivity.RESULT_KO)
        }
        activity?.finish()
    }

    private fun showPasswordDialog(inError: Boolean) {
        context?.let {
            MaterialAlertDialogBuilder(it).showPasswordDialog(
                layoutInflater,
                strings,
                inError,
            ) { pass ->
                viewModel.getBitmapFromProtectedPDF(it, pass)
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        activity?.setResult(Activity.RESULT_CANCELED)
        activity?.finish()
    }
}
