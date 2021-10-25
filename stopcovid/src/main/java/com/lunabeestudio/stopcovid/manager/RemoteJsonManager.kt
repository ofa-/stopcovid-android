package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.util.MalformedJsonException
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.stopcovid.model.BackendException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type

abstract class RemoteJsonManager<T>(serverManager: ServerManager) : RemoteFileManager(serverManager) {

    protected val gson: Gson = Gson()

    protected abstract val type: Type
    override val mimeType: String = "application/json"

    protected suspend fun loadLocal(context: Context): T? {
        return withContext(Dispatchers.IO) {
            getLocalFileOrAssetStream(context)?.use {
                it.readBytes()
            }?.toString(Charsets.UTF_8)?.let { fileString ->
                try {
                    gson.fromJson<T>(fileString, type)
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            }
        }
    }

    final override suspend fun fetchLast(context: Context): Boolean {
        return try {
            super.fetchLast(context)
        } catch (e: BackendException) {
            throw MalformedJsonException("Failed to parse JSON")
        }
    }

    final override suspend fun fileNotCorrupted(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                gson.fromJson<T>(file.readText(), type)
                true
            } catch (e: JsonSyntaxException) {
                false
            }
        }
    }
}