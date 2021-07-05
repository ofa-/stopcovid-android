package com.lunabeestudio.framework.remote.datasource

import android.content.Context
import com.lunabeestudio.analytics.model.AnalyticsServiceName
import com.lunabeestudio.domain.model.CacheConfig
import com.lunabeestudio.domain.model.Cluster
import com.lunabeestudio.domain.model.ClusterIndex
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.framework.remote.model.ApiWReportClea
import com.lunabeestudio.framework.remote.model.toDomain
import com.lunabeestudio.framework.remote.server.CleaReportApi
import com.lunabeestudio.framework.remote.server.CleaStatusApi
import com.lunabeestudio.framework.utils.RequestHelper
import com.lunabeestudio.robert.datasource.RemoteCleaDataSource
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import java.io.File

class CleaDataSource(
    val context: Context,
    cleaReportBaseUrl: String,
    private val cleaStatusFallbackBaseUrl: String,
) : RemoteCleaDataSource {

    private val cacheConfig = CacheConfig(File(context.cacheDir, "http_cache"), 30 * 1024 * 1024)

    private var filesDir = context.filesDir

    private fun getCleaStatusApi(cleaStatusBaseUrl: String): CleaStatusApi = RetrofitClient.getService(
        context = context,
        baseUrl = cleaStatusBaseUrl,
        clazz = CleaStatusApi::class.java,
        cacheConfig,
    )

    private var cleaReportApi: CleaReportApi = RetrofitClient.getService(
        context,
        cleaReportBaseUrl,
        CleaReportApi::class.java,
        null,
    )

    override suspend fun wreportClea(
        cleaApiVersion: String,
        token: String,
        pivotDate: Long,
        venueQrCodeList: List<VenueQrCode>,
    ): RobertResult {
        return RequestHelper.tryCatchRequest(context, filesDir, cleaApiVersion, AnalyticsServiceName.WREPORT) {
            cleaReportApi.wreport(cleaApiVersion, "Bearer $token", ApiWReportClea.fromVenueQrCodeList(pivotDate, venueQrCodeList))
        }
    }

    override suspend fun cleaClusterIndex(apiVersion: String, cleaStatusBaseUrl: String?): RobertResultData<ClusterIndex> {
        val result = RequestHelper.tryCatchRequestData(context, filesDir, apiVersion, null) {
            getCleaStatusApi(cleaStatusBaseUrl ?: cleaStatusFallbackBaseUrl).getClusterIndex(apiVersion)
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.toDomain())
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun cleaClusterList(
        apiVersion: String,
        iteration: String,
        clusterPrefix: String,
        cleaStatusBaseUrl: String?
    ): RobertResultData<List<Cluster>> {
        val result = RequestHelper.tryCatchRequestData(context, filesDir, apiVersion, null) {
            getCleaStatusApi(cleaStatusBaseUrl ?: cleaStatusFallbackBaseUrl).getClusterList(apiVersion, iteration, clusterPrefix)
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.mapNotNull { it.toDomain() })
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }
}
