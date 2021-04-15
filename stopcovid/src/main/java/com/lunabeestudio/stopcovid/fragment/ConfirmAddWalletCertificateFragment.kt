package com.lunabeestudio.stopcovid.fragment

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.dangerButtonItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class ConfirmAddWalletCertificateFragment : MainFragment() {

    private val args: ConfirmAddWalletCertificateFragmentArgs by navArgs()

    override fun getTitleKey(): String = "confirmWalletQrCodeController.title"

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.wallet
            identifier = R.drawable.wallet.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["confirmWalletQrCodeController.explanation.title"]
            gravity = Gravity.CENTER
            identifier = "confirmWalletQrCodeController.explanation.title".hashCode().toLong()
        }
        items += titleItem {
            text = strings["confirmWalletQrCodeController.explanation.subtitle"]
            gravity = Gravity.CENTER
            identifier = "confirmWalletQrCodeController.explanation.subtitle".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        items += buttonItem {
            text = strings["confirmWalletQrCodeController.confirm"]
            gravity = Gravity.CENTER
            width = ViewGroup.LayoutParams.MATCH_PARENT
            onClickListener = View.OnClickListener {
                setFragmentResult(WalletFragment.CONFIRM_ADD_CODE_RESULT_KEY,
                    bundleOf(WalletFragment.CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM to true,
                        WalletFragment.CONFIRM_ADD_CODE_BUNDLE_KEY_CODE to args.certificateCode))
                findNavControllerOrNull()?.navigateUp()
            }
            identifier = "confirmWalletQrCodeController.confirm".hashCode().toLong()
        }
        items += dangerButtonItem {
            text = strings["common.cancel"]
            gravity = Gravity.CENTER
            width = ViewGroup.LayoutParams.MATCH_PARENT
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.navigateUp()
            }
            identifier = "common.cancel".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        return items
    }

}