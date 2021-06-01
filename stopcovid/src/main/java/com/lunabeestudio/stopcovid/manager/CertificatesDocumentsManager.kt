package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.graphics.drawable.Drawable
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.extension.getFirstSupportedLanguage
import timber.log.Timber
import java.io.File

abstract class RemoteImageDocumentManager(private val context: Context) : RemoteFileManager() {
    abstract val remoteFileUrlTemplate: String

    final override val remoteFileUrl: String
        get() = remoteFileUrlTemplate.format(context.getFirstSupportedLanguage())
    final override val assetFilePath: String? = null

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

class CertificatesDocumentsManager(context: Context) {

    private val certificateDocumentRemoteFileManager = object : RemoteImageDocumentManager(context) {
        override val localFileName: String = ConfigConstant.Wallet.TEST_CERTIFICATE_THUMBNAIL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.TEST_CERTIFICATE_THUMBNAIL_TEMPLATE_URL
    }

    private val fullCertificateDocumentRemoteFileManager = object : RemoteImageDocumentManager(context) {
        override val localFileName: String = ConfigConstant.Wallet.TEST_CERTIFICATE_FULL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.TEST_CERTIFICATE_FULL_TEMPLATE_URL
    }

    private val vaccinDocumentRemoteFileManager = object : RemoteImageDocumentManager(context) {
        override val localFileName: String = ConfigConstant.Wallet.VACCIN_CERTIFICATE_THUMBNAIL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.VACCIN_CERTIFICATE_THUMBNAIL_TEMPLATE_URL
    }

    private val fullVaccinDocumentRemoteFileManager = object : RemoteImageDocumentManager(context) {
        override val localFileName: String = ConfigConstant.Wallet.VACCIN_CERTIFICATE_FULL_FILE
        override val remoteFileUrlTemplate: String = ConfigConstant.Wallet.VACCIN_CERTIFICATE_FULL_TEMPLATE_URL
    }

    suspend fun onAppForeground(context: Context) {
        fetchLastImages(context)
    }

    suspend fun fetchLastImages(context: Context): Boolean {
        return certificateDocumentRemoteFileManager.fetchLastImage(context) &&
            fullCertificateDocumentRemoteFileManager.fetchLastImage(context) &&
            vaccinDocumentRemoteFileManager.fetchLastImage(context) &&
            fullVaccinDocumentRemoteFileManager.fetchLastImage(context)
    }
}