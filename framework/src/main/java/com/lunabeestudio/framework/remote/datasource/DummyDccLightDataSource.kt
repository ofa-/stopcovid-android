package com.lunabeestudio.framework.remote.datasource

import com.lunabeestudio.domain.model.DccLightData
import com.lunabeestudio.robert.datasource.RemoteDccLightDataSource
import com.lunabeestudio.robert.model.ForbiddenException
import com.lunabeestudio.robert.model.RobertResultData

class DummyDccLightDataSource : RemoteDccLightDataSource {
    override suspend fun generateActivityPass(
        serverPublicKey: String,
        encodedCertificate: String,
    ): RobertResultData<DccLightData> = RobertResultData.Failure(ForbiddenException("Dummy datasource"))
}
