package com.lunabeestudio.robert.datasource

import com.lunabeestudio.domain.model.Cluster
import com.lunabeestudio.domain.model.ClusterIndex
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData

interface RemoteCleaDataSource {

    suspend fun wreportClea(cleaApiVersion: String, token: String, pivotDate: Long, venueQrCodeList: List<VenueQrCode>): RobertResult
    suspend fun cleaClusterIndex(apiVersion: String): RobertResultData<ClusterIndex>
    suspend fun cleaClusterList(apiVersion: String, iteration: String, clusterPrefix: String): RobertResultData<List<Cluster>>
}