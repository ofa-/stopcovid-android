package com.lunabeestudio.framework.remote.model

import com.lunabeestudio.domain.model.Cluster
import com.lunabeestudio.domain.model.ClusterExposure
import timber.log.Timber

internal class ApiCluster(
    val ltid: String?,
    val exp: List<ApiClusterExposure>?
)

internal class ApiClusterExposure(
    val s: String,
    val d: Long,
    val r: Float
)

internal fun ApiCluster.toDomain() = try {
    Cluster(
        ltid = ltid!!,
        exposures = exp?.map {
            ClusterExposure(
                startTimeNTP = it.s.toLong(),
                duration = it.d,
                riskLevel = it.r
            )
        }
    )
} catch (e: Exception) {
    Timber.e(e)
    null
}
