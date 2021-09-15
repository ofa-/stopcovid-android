package com.lunabeestudio.stopcovid.manager

import android.content.Context
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.stopcovid.model.BackendException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

abstract class RemoteFileManager(private val serverManager: ServerManager) {

    protected abstract fun getLocalFileName(context: Context): String
    protected abstract fun getRemoteFileUrl(context: Context): String
    protected abstract fun getAssetFilePath(context: Context): String?
    protected abstract val mimeType: String

    protected suspend fun loadLocalBytes(context: Context): ByteArray? {
        val localFile = File(context.filesDir, getLocalFileName(context))
        return loadLocalBytes(localFile, context)
    }

    protected suspend fun loadLocalBytes(
        localFile: File,
        context: Context
    ): ByteArray? {
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
            getAssetFilePath(context) != null -> {
                getDefaultAssetFile(context)
            }
            else -> {
                Timber.v("Nothing to load")
                null
            }
        }
    }

    private suspend fun getDefaultAssetFile(context: Context): ByteArray? {
        return getAssetFilePath(context)?.let { path ->
            withContext(Dispatchers.IO) {
                @Suppress("BlockingMethodInNonBlockingContext")
                context.assets.open(path).use { stream ->
                    stream.readBytes()
                }
            }
        }
    }

    protected open suspend fun fetchLast(context: Context): Boolean {
        val tmpFileName = "${getLocalFileName(context)}.bck"
        val tmpFile = File(context.filesDir, tmpFileName)

        return try {
            val remoteFileUrl = getRemoteFileUrl(context)
            Timber.v("Fetching remote data at $remoteFileUrl")
            val saveSucceeded = serverManager.saveTo(remoteFileUrl, tmpFile, mimeType)
            if (saveSucceeded) {
                if (fileNotCorrupted(tmpFile)) {
                    tmpFile.copyTo(File(context.filesDir, getLocalFileName(context)), overwrite = true, bufferSize = 4 * 1024)
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
        File(context.filesDir, getLocalFileName(context)).delete()
    }
}
