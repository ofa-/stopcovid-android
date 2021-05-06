package com.lunabeestudio.framework.remote.model

import com.lunabeestudio.domain.model.ClusterIndex

internal class ApiClusterIndex(
    val i: Int,
    val c: List<String>
)

internal fun ApiClusterIndex.toDomain() = ClusterIndex(
    iteration = i,
    clusterPrefixList = c
)
