/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/01/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.ImportQRCodeActivity
import com.lunabeestudio.stopcovid.activity.MainActivity

abstract class QRCodeDccFragment : QRCodeFragment() {

    protected val pickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { activityResult ->
            when (activityResult.resultCode) {
                Activity.RESULT_OK -> activityResult.data?.getStringExtra(ImportQRCodeActivity.EXTRA_CODE_SCANNED)?.let { code ->
                    onCodeScanned(code)
                }
                ImportQRCodeActivity.RESULT_KO -> strings["universalQrScanController.error.noCodeFound"]?.let { str ->
                    (activity as? MainActivity)?.showErrorSnackBar(str)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.import_qr_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.item_import).title = strings["universalQrScanController.rightBarButton.title"]
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.item_import) {
            context?.let { context ->
                val intent = Intent(context, ImportQRCodeActivity::class.java)
                pickerLauncher.launch(intent)
            }
            true
        } else {
            false
        }
    }
}