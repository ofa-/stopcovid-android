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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.NavigationUI
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.appbar.AppBarLayout
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.databinding.FragmentRecyclerViewKonfettiBinding
import com.lunabeestudio.stopcovid.extension.emitDefaultKonfetti
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.vaccineDate
import com.lunabeestudio.stopcovid.extension.vaccineDose
import com.lunabeestudio.stopcovid.extension.vaccineMedicinalProduct
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.viewmodel.VaccineCompletionViewModel
import com.lunabeestudio.stopcovid.viewmodel.VaccineCompletionViewModelFactory
import com.lunabeestudio.stopcovid.worker.VaccineCompletedNotificationWorker
import com.mikepenz.fastadapter.GenericItem
import nl.dionsegijn.konfetti.ParticleSystem
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class VaccineCompletionFragment : MainFragment() {

    private var fragmentRecyclerViewKonfettiBinding: FragmentRecyclerViewKonfettiBinding? = null

    override val layout: Int = R.layout.fragment_recycler_view_konfetti

    override fun getAppBarLayout(): AppBarLayout? {
        return fragmentRecyclerViewKonfettiBinding?.appBarLayout
    }

    private val args by navArgs<VaccineCompletionFragmentArgs>()

    private val configuration by lazy {
        requireContext().robertManager().configuration
    }

    private val viewModel: VaccineCompletionViewModel by viewModels {
        VaccineCompletionViewModelFactory(
            args.certificateId,
            walletRepository,
        )
    }

    private val longDateFormat = SimpleDateFormat.getDateInstance(DateFormat.LONG)

    @SuppressLint("SimpleDateFormat")
    private val noYearDateFormat = SimpleDateFormat("d MMMM")

    private var reminderSet: Boolean = false
    private var konfettiEmitted = false
    private var konfettis: ParticleSystem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        konfettiEmitted = savedInstanceState?.getBoolean(SAVE_INSTANCE_KONFETTI_EMITTED) ?: konfettiEmitted
        reminderSet = savedInstanceState?.getBoolean(SAVE_INSTANCE_REMINDER_SET) ?: reminderSet
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        fragmentRecyclerViewKonfettiBinding = view?.let { FragmentRecyclerViewKonfettiBinding.bind(it) }
        setupToolbar()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.certificate.observe(viewLifecycleOwner) { certificate ->
            val vaccination = (certificate as? EuropeanCertificate)?.greenCertificate?.vaccinations?.lastOrNull()
            if (vaccination != null && vaccination.doseNumber >= vaccination.totalSeriesOfDoses) {
                refreshScreen()
            } else if (vaccination != null && vaccination.doseNumber < vaccination.totalSeriesOfDoses) {
                Timber.e("Unexpected vaccination $vaccination")
                findNavControllerOrNull()?.navigateUp()
            } else {
                showLoading()
            }
        }
        viewModel.showWalletEvent.observe(viewLifecycleOwner) {
            findNavControllerOrNull()?.popBackStack()
        }
    }

    private fun setupToolbar() {
        // remove shadow
        fragmentRecyclerViewKonfettiBinding?.appBarLayout?.outlineProvider = null
        // replace previous activity action bar
        appCompatActivity?.setSupportActionBar(fragmentRecyclerViewKonfettiBinding?.toolbar)
        // show back arrow
        appCompatActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // no title
        appCompatActivity?.supportActionBar?.title = null

        fragmentRecyclerViewKonfettiBinding?.toolbar?.let { toolbar ->
            findNavControllerOrNull()?.let { navController ->
                NavigationUI.setupWithNavController(
                    toolbar,
                    navController
                )
            }
        }
    }

    private fun emitKonfetti() {
        requireView().performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
        konfettis?.stop()
        konfettis = fragmentRecyclerViewKonfettiBinding?.konfettiView?.emitDefaultKonfetti(binding)
    }

    override fun getTitleKey(): String = ""

    override suspend fun getItems(): List<GenericItem> {
        val greenCertificate = (viewModel.certificate.value as? EuropeanCertificate)?.greenCertificate
        val vaccineDate = greenCertificate?.vaccineDate ?: return emptyList()
        val vaccineMedicinalProduct: String? = greenCertificate.vaccineMedicinalProduct
        val vaccineDoseNumber = greenCertificate.vaccineDose?.first ?: 0
        val noWaitDoses: Boolean = (
            configuration.noWaitDoses[vaccineMedicinalProduct]
                ?: configuration.noWaitDoses[DEFAULT_KEY]
            )
            ?.let { vaccineDoseNumber >= it }
            ?: false
        val daysAfterCompletion = try {
            configuration.daysAfterCompletion[vaccineMedicinalProduct]
                ?: configuration.daysAfterCompletion[DEFAULT_KEY]
                ?: return emptyList()
        } catch (e: NullPointerException) {
            Timber.e(e)
            return emptyList()
        }
        val completedDate = Calendar.getInstance().apply {
            time = vaccineDate
            add(Calendar.DAY_OF_YEAR, daysAfterCompletion)
        }.time
        val isVaccineCompleted = (noWaitDoses || completedDate <= Date())

        val items = mutableListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.ic_thumbsup
            imageTint = R.attr.colorPrimary.fetchSystemColor(requireContext())
            onClick = {
                emitKonfetti()
            }
            identifier = R.drawable.ic_thumbsup.toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }

        val stringStateKey = if (isVaccineCompleted) {
            COMPLETED_STRING_KEY
        } else {
            PENDING_STRING_KEY
        }

        if (!konfettiEmitted) {
            konfettiEmitted = true
            emitKonfetti()
        }

        items += cardWithActionItem {
            val formattedDate = longDateFormat.format(completedDate)
            val explanations = listOfNotNull(
                stringsFormat("vaccineCompletionController.noWait.explanation.body", vaccineDoseNumber - 1).takeIf { noWaitDoses },
                stringsFormat("vaccineCompletionController.$stringStateKey.explanation.body", formattedDate),
            )
            mainTitle = stringsFormat("vaccineCompletionController.$stringStateKey.explanation.title", formattedDate)
            mainBody = explanations.joinToString("\n\n")
            identifier = "vaccineCompletionController.$stringStateKey.explanation.title".hashCode().toLong()
        }

        if (!isVaccineCompleted && !reminderSet) {
            items.addNotifyItems(completedDate, stringStateKey)
        }

        if (isVaccineCompleted || reminderSet) {
            items.addFavoriteItems()
        } else {
            items.addNotifyAndFavoriteItems(completedDate)
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
        }

        return items
    }

    private fun MutableList<GenericItem>.addNotifyItems(completedDate: Date, stringStateKey: String) {
        this += spaceItem {
            spaceRes = R.dimen.spacing_large
        }
        this += buttonItem {
            text = stringsFormat(
                "vaccineCompletionController.$stringStateKey.button.notifyMe.title",
                noYearDateFormat.format(completedDate)
            )
            width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomMarginRes = null
            onClickListener = View.OnClickListener {
                context?.let {
                    if (!reminderSet) {
                        notifyMe(it, completedDate)
                    }
                }
            }
            identifier = "vaccineCompletionController.$stringStateKey.button.notifyMe.title".hashCode().toLong()
        }
        this += captionItem {
            text = stringsFormat(
                "vaccineCompletionController.footer.notify",
                noYearDateFormat.format(completedDate)
            )
            textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
            identifier = "vaccineCompletionController.footer.notify".hashCode().toLong()
        }
    }

    private fun MutableList<GenericItem>.addNotifyAndFavoriteItems(completedDate: Date) {
        this += spaceItem {
            spaceRes = R.dimen.spacing_large
        }
        this += buttonItem {
            text = strings["vaccineCompletionController.button.notifyAndFavorite.title"]
            width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomMarginRes = null
            onClickListener = View.OnClickListener {
                context?.let {
                    if (!reminderSet) {
                        notifyMe(it, completedDate)
                    }
                }
                viewModel.addCertificateInFavorite()
            }
            identifier = "vaccineCompletionController.button.notifyAndFavorite.title".hashCode().toLong()
        }
        this += captionItem {
            text = stringsFormat(
                "vaccineCompletionController.footer.notifyAndFavorite",
                noYearDateFormat.format(completedDate)
            )
            textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
            identifier = "vaccineCompletionController.footer.notifyAndFavorite".hashCode().toLong()
        }
    }

    private fun MutableList<GenericItem>.addFavoriteItems() {
        this += spaceItem {
            spaceRes = R.dimen.spacing_large
        }
        this += buttonItem {
            text = strings["vaccineCompletionController.button.favorite.title"]
            width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomMarginRes = null
            onClickListener = View.OnClickListener {
                viewModel.addCertificateInFavorite()
            }
            identifier = "vaccineCompletionController.button.favorite.title".hashCode().toLong()
        }
        this += captionItem {
            text = strings["vaccineCompletionController.footer.favorite"]
            textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
            identifier = "vaccineCompletionController.footer.favorite".hashCode().toLong()
        }
    }

    private fun notifyMe(context: Context, completedDate: Date) {
        val reminderWorker = OneTimeWorkRequestBuilder<VaccineCompletedNotificationWorker>()
            .setInitialDelay(completedDate.time - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(Constants.WorkerNames.VACCINATION_COMPLETED_REMINDER, ExistingWorkPolicy.REPLACE, reminderWorker)

        strings["common.notifyMe.feedback"]?.let { message ->
            (activity as? MainActivity)?.showSnackBar(message)
        }

        reminderSet = true
        refreshScreen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(SAVE_INSTANCE_KONFETTI_EMITTED, konfettiEmitted)
        outState.putBoolean(SAVE_INSTANCE_REMINDER_SET, reminderSet)
    }

    companion object {
        private const val DEFAULT_KEY = "DEFAULT"
        private const val PENDING_STRING_KEY = "pending"
        private const val COMPLETED_STRING_KEY = "completed"

        private const val SAVE_INSTANCE_REMINDER_SET = "save.instance.reminderSet"
        private const val SAVE_INSTANCE_KONFETTI_EMITTED = "save.instance.konfettiEmitted"
    }
}