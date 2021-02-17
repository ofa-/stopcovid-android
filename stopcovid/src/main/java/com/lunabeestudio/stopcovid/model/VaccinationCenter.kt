/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/04/02 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
class VaccinationCenter(
    @SerializedName("nom")
    val name: String,
    @SerializedName("adresseNum")
    val streetNumber: String,
    @SerializedName("adresseVoie")
    val streetName: String,
    @SerializedName("communeCP")
    val postalCode: String,
    @SerializedName("communeNom")
    val locality: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("rdvURL")
    val url: String?,
    @SerializedName("rdvTel")
    val phone: String?,
    @SerializedName("rdvModalites")
    val modalities: String?,
    @SerializedName("rdvPlanning")
    val planning: String?,
    @SerializedName("dateOuverture")
    val openingTimestamp: Long?,
) : Parcelable