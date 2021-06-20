package com.lunabeestudio.robert.repository

import com.lunabeestudio.domain.model.Cluster
import com.lunabeestudio.domain.model.ClusterIndex
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.RemoteCleaDataSource
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData

class CleaRepository(
    private val cleaDataSource: RemoteCleaDataSource,
    private val localKeystoreDataSource: LocalKeystoreDataSource,
) {

    suspend fun wreportClea(cleaApiVersion: String, token: String, pivotDateNTP: Long, venueQrCodeList: List<VenueQrCode>): RobertResult =
        cleaDataSource.wreportClea(cleaApiVersion, token, pivotDateNTP, venueQrCodeList)

    suspend fun cleaClusterIndex(cleaApiVersion: String): RobertResultData<ClusterIndex> {
        return cleaDataSource.cleaClusterIndex(cleaApiVersion, getRandomCleaStatusBaseUrl())
    }

    suspend fun cleaClusterList(cleaApiVersion: String, iteration: String, prefix: String): RobertResultData<List<Cluster>> =
        cleaDataSource.cleaClusterList(cleaApiVersion, iteration, prefix, getRandomCleaStatusBaseUrl())

    private fun getRandomCleaStatusBaseUrl(): String? = localKeystoreDataSource.configuration?.cleaUrls?.randomOrNull()
}