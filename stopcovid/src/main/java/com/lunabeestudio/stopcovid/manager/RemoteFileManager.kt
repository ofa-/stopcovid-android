package com.lunabeestudio.stopcovid.manager

import android.content.Context
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import com.lunabeestudio.stopcovid.model.BackendException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

abstract class RemoteFileManager {

    protected abstract val localFileName: String
    protected abstract val remoteFileUrl: String
    protected abstract val assetFilePath: String?

    protected suspend fun loadLocalBytes(context: Context): ByteArray? {
        val localFile = File(context.filesDir, localFileName)
        return when {
            localFile.exists() -> {
                withContext(Dispatchers.IO) {
                    try {
                        Timber.v("Loading $localFile as ByteArray")
                        localFile.readBytes()
                    } catch (e: Exception) {
                        Timber.e(e)
                        null
                    }
                }
            }
            assetFilePath != null -> {
                getDefaultAssetFile(context)
            }
            else -> {
                Timber.v("Nothing to load")
                null
            }
        }
    }

    private suspend fun getDefaultAssetFile(context: Context): ByteArray? {
        return assetFilePath?.let { path ->
            withContext(Dispatchers.IO) {
                @Suppress("BlockingMethodInNonBlockingContext")
                context.assets.open(path).use { stream ->
                    stream.readBytes()
                }
            }
        }
    }

    protected open suspend fun fetchLast(context: Context): Boolean {
        val tmpFileName = "$localFileName.bck"
        val tmpFile = File(context.filesDir, tmpFileName)

        return try {
            Timber.v("Fetching remote data at $remoteFileUrl")
            if (remoteFileUrl.saveTo(context, tmpFile)) {
                if (fileNotCorrupted(tmpFile)) {
                    tmpFile.copyTo(File(context.filesDir, localFileName), overwrite = true, bufferSize = 4 * 1024)
                } else {
                    throw BackendException("$tmpFile is corrupted")
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Fetching failed")
            false
        } finally {
            tmpFile.delete()
        }
    }

    abstract suspend fun fileNotCorrupted(file: File): Boolean

    fun clearLocal(context: Context) {
        File(context.filesDir, localFileName).delete()
    }
}
