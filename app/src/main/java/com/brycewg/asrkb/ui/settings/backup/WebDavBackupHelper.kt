/**
 * WebDAV 备份公共辅助，使用 OkHttp 完成设置备份上传与恢复。
 *
 * 归属模块：ui/settings/backup
 */
package com.brycewg.asrkb.ui.settings.backup

import android.content.Context
import android.util.Log
import com.brycewg.asrkb.store.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object WebDavBackupHelper {
    private const val TAG = "WebDavBackupHelper"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private const val WEBDAV_DIRECTORY = "BiBiKeyboard"
    private const val WEBDAV_FILENAME = "asr_keyboard_settings.json"
    private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
    private val webDavClient by lazy { OkHttpClient.Builder().build() }

    sealed class UploadResult {
        object Success : UploadResult()
        data class Error(
            val statusCode: Int?,
            val responsePhrase: String?,
            val throwable: Throwable?
        ) : UploadResult()
    }

    sealed class DownloadResult {
        data class Success(val json: String) : DownloadResult()
        object NotFound : DownloadResult()
        data class Error(
            val statusCode: Int?,
            val responsePhrase: String?,
            val throwable: Throwable?
        ) : DownloadResult()
    }

    fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/')
    fun buildDirectoryUrl(baseUrl: String): String = "$baseUrl/$WEBDAV_DIRECTORY/"
    fun buildFileUrl(baseUrl: String): String = "$baseUrl/$WEBDAV_DIRECTORY/$WEBDAV_FILENAME"

    /**
     * 将当前偏好（含密钥）导出为 JSON 并通过 WebDAV 上传到固定路径。
     * @return true 表示上传成功；false 表示参数不全或上传失败。
     */
    suspend fun uploadSettings(context: Context, prefs: Prefs): Boolean = when (uploadSettingsWithStatus(context, prefs)) {
        is UploadResult.Success -> true
        is UploadResult.Error -> false
    }

    /**
     * 带详细状态的上传版本，便于 UI 显示具体错误信息。
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun uploadSettingsWithStatus(context: Context, prefs: Prefs): UploadResult = withContext(Dispatchers.IO) {
        val rawUrl = prefs.webdavUrl.trim()
        if (rawUrl.isEmpty()) {
            return@withContext UploadResult.Error(null, "EMPTY_URL", null)
        }
        val baseUrl = normalizeBaseUrl(rawUrl)

        val upload = runCatching { performWebDavUpload(baseUrl, prefs) }
        if (upload.isSuccess) {
            UploadResult.Success
        } else {
            val error = upload.exceptionOrNull()
            Log.e(TAG, "WebDAV upload error: ${error?.message}", error)
            UploadResult.Error(extractStatusCode(error), extractResponsePhrase(error), error)
        }
    }

    /**
     * 从 WebDAV 下载备份 JSON 文本。
     * @return JSON 字符串；若未配置 URL 或下载失败返回 null。
     */
    suspend fun downloadSettings(prefs: Prefs): String? = when (val result = downloadSettingsWithStatus(prefs)) {
        is DownloadResult.Success -> result.json
        is DownloadResult.NotFound -> null
        is DownloadResult.Error -> null
    }

    /**
     * 带详细状态的下载版本，便于 UI 显示具体错误信息（包括 404 备份缺失）。
     */
    suspend fun downloadSettingsWithStatus(prefs: Prefs): DownloadResult = withContext(Dispatchers.IO) {
        val rawUrl = prefs.webdavUrl.trim()
        if (rawUrl.isEmpty()) {
            return@withContext DownloadResult.Error(null, "EMPTY_URL", null)
        }
        val baseUrl = normalizeBaseUrl(rawUrl)
        val fileUrl = buildFileUrl(baseUrl)

        val download = runCatching { performWebDavDownload(fileUrl, prefs) }
        download.getOrNull()?.let { return@withContext DownloadResult.Success(it) }

        val error = download.exceptionOrNull()
        val statusCode = extractStatusCode(error)
        if (statusCode == 404) {
            Log.w(TAG, "WebDAV backup not found at $fileUrl: ${error?.message}")
            return@withContext DownloadResult.NotFound
        }

        if (error != null) {
            Log.e(TAG, "WebDAV download error: ${error.message}", error)
        }

        DownloadResult.Error(statusCode, extractResponsePhrase(error), error)
    }

    private fun performWebDavUpload(baseUrl: String, prefs: Prefs) {
        ensureDirectoryExists(prefs, baseUrl)

        val payload = prefs.exportJsonString()
        val fileUrl = buildFileUrl(baseUrl)
        val requestBuilder = Request.Builder()
            .url(fileUrl)
            .put(payload.toByteArray(Charsets.UTF_8).toRequestBody(JSON_MEDIA))
        addBasicAuthIfNeeded(requestBuilder, prefs)

        webDavClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw WebDavHttpException(response.code, response.message)
            }
        }
    }

    private fun performWebDavDownload(fileUrl: String, prefs: Prefs): String {
        val requestBuilder = Request.Builder().url(fileUrl)
        addBasicAuthIfNeeded(requestBuilder, prefs)

        webDavClient.newCall(requestBuilder.build()).execute().use { response ->
            return when {
                response.code == 404 -> throw WebDavHttpException(response.code, response.message)
                response.isSuccessful -> response.body.string().orEmpty()
                else -> throw WebDavHttpException(response.code, response.message)
            }
        }
    }

    private fun ensureDirectoryExists(prefs: Prefs, baseUrl: String) {
        val dirUrl = buildDirectoryUrl(baseUrl)
        val checkRequestBuilder = Request.Builder()
            .url(dirUrl)
            .method("PROPFIND", EMPTY_BODY)
            .addHeader("Depth", "0")
        addBasicAuthIfNeeded(checkRequestBuilder, prefs)

        webDavClient.newCall(checkRequestBuilder.build()).execute().use { response ->
            when (response.code) {
                200, 207 -> return
                301, 302, 307, 308, 405 -> return
                404 -> Unit
                else -> throw WebDavHttpException(response.code, response.message)
            }
        }

        val mkdirRequestBuilder = Request.Builder()
            .url(dirUrl)
            .method("MKCOL", EMPTY_BODY)
        addBasicAuthIfNeeded(mkdirRequestBuilder, prefs)

        webDavClient.newCall(mkdirRequestBuilder.build()).execute().use { response ->
            when (response.code) {
                200, 201, 204, 405 -> return
                else -> throw WebDavHttpException(response.code, response.message)
            }
        }
    }

    private fun addBasicAuthIfNeeded(builder: Request.Builder, prefs: Prefs) {
        val user = prefs.webdavUsername.trim()
        val pass = prefs.webdavPassword.trim()
        if (user.isNotEmpty()) {
            try {
                val token = Credentials.basic(user, pass, Charsets.UTF_8)
                builder.header("Authorization", token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to append basic auth header", e)
            }
        }
    }

    private fun extractStatusCode(t: Throwable?): Int? = when (t) {
        is WebDavHttpException -> t.statusCode
        else -> null
    }

    private fun extractResponsePhrase(t: Throwable?): String? = when (t) {
        is WebDavHttpException -> t.responsePhrase
        else -> t?.message
    }

    private class WebDavHttpException(
        val statusCode: Int,
        val responsePhrase: String?
    ) : Exception("HTTP $statusCode${responsePhrase?.let { " $it" }.orEmpty()}")
}
