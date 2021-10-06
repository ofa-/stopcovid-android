package com.lunabeestudio.framework.remote.datasource

import android.content.Context
import com.lunabeestudio.analytics.manager.AnalyticsManager
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
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.framework.utils.RequestHelper
import com.lunabeestudio.robert.datasource.RemoteCleaDataSource
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import okhttp3.OkHttpClient
import java.io.File

class CleaDataSource(
    context: Context,
    cleaReportBaseUrl: String,
    private val cleaStatusFallbackBaseUrl: String,
    private val analyticsManager: AnalyticsManager,
) : RemoteCleaDataSource {

    private val cacheConfig = CacheConfig(
        File(context.cacheDir, ServerManager.OKHTTP_CACHE_FILENAME),
        ServerManager.OKHTTP_MAX_CACHE_SIZE_BYTES
    )

    private val okHttpClient: OkHttpClient = RetrofitClient.getDefaultOKHttpClient(context, cacheConfig)

    private var filesDir = context.filesDir

    private var cachedApi: Pair<String, CleaStatusApi>? = null

    private fun getCleaStatusApi(cleaStatusBaseUrl: String): CleaStatusApi {
        val cachedApi = cachedApi
        return if (cachedApi?.first == cleaStatusBaseUrl) {
            cachedApi.second
        } else {
            RetrofitClient.getService(
                baseUrl = cleaStatusBaseUrl,
                clazz = CleaStatusApi::class.java,
                okhttpClient = okHttpClient,
            ).also { cleaStatusApi ->
                this.cachedApi = cleaStatusBaseUrl to cleaStatusApi
            }
        }
    }

    private var cleaReportApi: CleaReportApi = RetrofitClient.getService(
        baseUrl = cleaReportBaseUrl,
        clazz = CleaReportApi::class.java,
        okhttpClient = okHttpClient,
    )

    override suspend fun wreportClea(
        cleaApiVersion: String,
        token: String,
        pivotDate: Long,
        venueQrCodeList: List<VenueQrCode>,
    ): RobertResult {
        return RequestHelper.tryCatchRequest(filesDir, cleaApiVersion, AnalyticsServiceName.WREPORT, analyticsManager) {
            cleaReportApi.wreport(cleaApiVersion, "Bearer $token", ApiWReportClea.fromVenueQrCodeList(pivotDate, venueQrCodeList))
        }
    }

    override suspend fun cleaClusterIndex(apiVersion: String, cleaStatusBaseUrl: String?): RobertResultData<ClusterIndex> {
        val result = RequestHelper.tryCatchRequestData(filesDir, apiVersion, null, analyticsManager) {
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
        val result = RequestHelper.tryCatchRequestData(filesDir, apiVersion, null, analyticsManager) {
            getCleaStatusApi(cleaStatusBaseUrl ?: cleaStatusFallbackBaseUrl).getClusterList(apiVersion, iteration, clusterPrefix)
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.mapNotNull { it.toDomain() })
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }
}
