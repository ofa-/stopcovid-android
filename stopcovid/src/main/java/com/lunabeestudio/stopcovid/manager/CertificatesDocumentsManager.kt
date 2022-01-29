package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.graphics.drawable.Drawable
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLanguage
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber
import java.io.File

abstract class RemoteImageDocumentManager(serverManager: ServerManager) : RemoteFileManager(serverManager) {
    abstract val remoteFileUrlTemplate: String
    override val mimeType: String = "image/png"

    override fun getRemoteFileUrl(context: Context): String = remoteFileUrlTemplate.format(context.getApplicationLanguage())
    final override fun getAssetFilePath(context: Context): String? = null

    final override suspend fun fileNotCorrupted(file: File): Boolean {
        return try {
            Drawable.createFromPath(file.path) != null
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    internal suspend fun fetchLastImage(context: Context): Boolean {
        return super.fetchLast(context)
    }
}

class CertificatesDocumentsManager(serverManager: ServerManager) {

    private val fetchMtx: Mutex = Mutex()

    private val certificateDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.TEST_CERTIFICATE_THUMBNAIL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.TEST_CERTIFICATE_THUMBNAIL_TEMPLATE_URL
    }

    private val fullCertificateDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.TEST_CERTIFICATE_FULL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.TEST_CERTIFICATE_FULL_TEMPLATE_URL
    }

    private val vaccinDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.VACCIN_CERTIFICATE_THUMBNAIL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.VACCIN_CERTIFICATE_THUMBNAIL_TEMPLATE_URL
    }

    private val fullVaccinDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.VACCIN_CERTIFICATE_FULL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.VACCIN_CERTIFICATE_FULL_TEMPLATE_URL
    }

    private val vaccinEuropeDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.VACCIN_EUROPE_CERTIFICATE_THUMBNAIL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.VACCIN_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_URL
    }

    private val fullVaccinEuropeDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.VACCIN_EUROPE_CERTIFICATE_FULL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.VACCIN_EUROPE_CERTIFICATE_FULL_TEMPLATE_URL
    }

    private val certificateEuropeDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.TEST_EUROPE_CERTIFICATE_THUMBNAIL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.TEST_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_URL
    }

    private val fullCertificateEuropeDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.TEST_EUROPE_CERTIFICATE_FULL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.TEST_EUROPE_CERTIFICATE_FULL_TEMPLATE_URL
    }

    private val recoveryEuropeDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.RECOVERY_EUROPE_CERTIFICATE_THUMBNAIL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.RECOVERY_EUROPE_CERTIFICATE_THUMBNAIL_TEMPLATE_URL
    }

    private val fullRecoveryEuropeDocumentRemoteFileManager = object : RemoteImageDocumentManager(serverManager) {
        override fun getLocalFileName(context: Context): String = ConfigConstant.Wallet.RECOVERY_EUROPE_CERTIFICATE_FULL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.RECOVERY_EUROPE_CERTIFICATE_FULL_TEMPLATE_URL
    }

    suspend fun onAppForeground(context: Context) {
        fetchLastImages(context)
    }

    suspend fun fetchLastImages(context: Context): Boolean {
        return if (fetchMtx.tryLock()) {
            try {
                certificateDocumentRemoteFileManager.fetchLastImage(context)
                    .and(fullCertificateDocumentRemoteFileManager.fetchLastImage(context))
                    .and(vaccinDocumentRemoteFileManager.fetchLastImage(context))
                    .and(fullVaccinDocumentRemoteFileManager.fetchLastImage(context))
                    .and(vaccinEuropeDocumentRemoteFileManager.fetchLastImage(context))
                    .and(fullVaccinEuropeDocumentRemoteFileManager.fetchLastImage(context))
                    .and(certificateEuropeDocumentRemoteFileManager.fetchLastImage(context))
                    .and(fullCertificateEuropeDocumentRemoteFileManager.fetchLastImage(context))
                    .and(recoveryEuropeDocumentRemoteFileManager.fetchLastImage(context))
                    .and(fullRecoveryEuropeDocumentRemoteFileManager.fetchLastImage(context))
            } finally {
                fetchMtx.unlock()
            }
        } else {
            false
        }
    }
}
