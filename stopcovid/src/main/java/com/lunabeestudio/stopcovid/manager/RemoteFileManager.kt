package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.util.MalformedJsonException
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type

abstract class RemoteFileManager<T> {

    private var gson: Gson = Gson()

    protected abstract val type: Type
    protected abstract val localFileName: String
    protected abstract val remoteFileUrl: String
    protected abstract val assetFilePath: String?

    protected suspend fun loadLocal(context: Context): T? {
        val localFile = File(context.filesDir, localFileName)
        return if (localFile.exists()) {
            withContext(Dispatchers.IO) {
                try {
                    Timber.v("Loading $localFile to object")
                    gson.fromJson<T>(localFile.readText(), type)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
        } else if (assetFilePath != null) {
            getDefaultAssetFile(context)
        } else {
            Timber.v("Nothing to load")
            null
        }
    }

    private suspend fun getDefaultAssetFile(context: Context): T? {
        return withContext(Dispatchers.IO) {
            @Suppress("BlockingMethodInNonBlockingContext")
            gson.fromJson<T>(context.assets.open(assetFilePath!!).use {
                it.readBytes().toString(Charsets.UTF_8)
            }, type)
        }
    }

    protected suspend fun fetchLast(context: Context): Boolean {
        val tmpFileName = "${localFileName}.bck"
        val tmpFile = File(context.filesDir, tmpFileName)

        return try {
            Timber.v("Fetching remote data at $remoteFileUrl")
            remoteFileUrl.saveTo(context, tmpFile)
            if (fileNotCorrupted(tmpFile)) {
                tmpFile.copyTo(File(context.filesDir, localFileName), overwrite = true, bufferSize = 4 * 1024)
            } else {
                throw MalformedJsonException("Failed to parse JSON")
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Fetching failed")
            false
        } finally {
            tmpFile.delete()
        }
    }

    private suspend fun fileNotCorrupted(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                gson.fromJson<T>(file.readText(), type)
                true
            } catch (e: JsonSyntaxException) {
                false
            }
        }
    }

    fun clearLocal(context: Context) {
        File(context.filesDir, localFileName).delete()
    }
}