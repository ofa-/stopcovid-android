/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/20 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.MultipassProfile
import com.lunabeestudio.stopcovid.model.UnknownException
import com.lunabeestudio.stopcovid.usecase.GenerateMultipassUseCase
import com.lunabeestudio.stopcovid.usecase.GetFilteredMultipassProfileFromIdUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MultipassCertificatesPickerViewModel(
    profileId: String,
    private val getFilteredMultipassProfileFromIdUseCase: GetFilteredMultipassProfileFromIdUseCase,
    private val generateMultipassUseCase: GenerateMultipassUseCase,
    private val robertManager: RobertManager,
) : ViewModel() {

    private val selectedItems: MutableSet<String> = mutableSetOf()

    val multipass: LiveData<MultipassProfile?> = liveData {
        val profile = getFilteredMultipassProfileFromIdUseCase(profileId)
        emit(profile)
        if ((profile?.certificates?.size ?: 0) < robertManager.configuration.multipassConfig?.minDcc ?: 0) {
            _uiModel.value = MultipassCertificatesPickerUiModel(
                bottomButtonState = MultipassCertificatesPickerUiModel.ValidateButtonState.DISABLED,
                profileNoDccMin = Event(Unit),
                selectionMaxReached = null,
            )
        }
    }

    private val _uiModel: MutableLiveData<MultipassCertificatesPickerUiModel> = MutableLiveData(
        MultipassCertificatesPickerUiModel(
            bottomButtonState = MultipassCertificatesPickerUiModel.ValidateButtonState.DISABLED,
            profileNoDccMin = null,
            selectionMaxReached = null,
        )
    )
    val uiModel: LiveData<MultipassCertificatesPickerUiModel>
        get() = _uiModel

    fun toggleCertificate(certificate: EuropeanCertificate) {
        val multipassConfig = robertManager.configuration.multipassConfig
        if (multipassConfig != null) {
            if (!selectedItems.contains(certificate.id) && selectedItems.size == multipassConfig.maxDcc) {
                _uiModel.value = _uiModel.value?.copy(selectionMaxReached = Event(Unit))
            } else {
                if (!selectedItems.add(certificate.id)) {
                    selectedItems.remove(certificate.id)
                }
                updateValidateButtonState()
            }
        } else {
            // Unexpected case
            if (!selectedItems.add(certificate.id)) {
                selectedItems.remove(certificate.id)
            }
        }
    }

    fun isCertificateSelected(certificate: EuropeanCertificate): Boolean = selectedItems.contains(certificate.id)

    fun generateMultipass(): Flow<TacResult<EuropeanCertificate>> {
        return multipass.value?.certificates?.filter { selectedItems.contains(it.id) }?.let { generateMultipassUseCase(it) }
            ?: flowOf(TacResult.Failure(UnknownException("No certificate found for the current profile")))
    }

    private fun updateValidateButtonState() {
        val multipassConfig = robertManager.configuration.multipassConfig ?: return
        val bottomButtonState = when {
            multipassConfig.minDcc <= selectedItems.size && selectedItems.size <= multipassConfig.maxDcc ->
                MultipassCertificatesPickerUiModel.ValidateButtonState.ENABLED
            else ->
                MultipassCertificatesPickerUiModel.ValidateButtonState.DISABLED
        }
        _uiModel.value = _uiModel.value?.copy(bottomButtonState = bottomButtonState)
    }
}

data class MultipassCertificatesPickerUiModel(
    val bottomButtonState: ValidateButtonState,
    val profileNoDccMin: Event<Unit>?,
    val selectionMaxReached: Event<Unit>?,
) {
    enum class ValidateButtonState {
        DISABLED, ENABLED
    }
}

class MultipassCertificatesPickerViewModelFactory(
    private val profileId: String,
    private val getFilteredMultipassProfileFromIdUseCase: GetFilteredMultipassProfileFromIdUseCase,
    private val generateMultipassUseCase: GenerateMultipassUseCase,
    private val robertManager: RobertManager,
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MultipassCertificatesPickerViewModel(
            profileId = profileId,
            getFilteredMultipassProfileFromIdUseCase = getFilteredMultipassProfileFromIdUseCase,
            generateMultipassUseCase = generateMultipassUseCase,
            robertManager = robertManager,
        ) as T
    }
}