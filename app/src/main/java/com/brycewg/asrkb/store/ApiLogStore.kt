/**
 * API 调用摘要日志存储，用于本地排查 ASR/LLM 外部请求问题。
 *
 * 归属模块：store
 */
package com.brycewg.asrkb.store

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ApiLogStore {
    private const val TAG = "ApiLogStore"
    private const val SP_NAME = "asr_prefs"
    private const val KEY_API_LOG_JSON = "api_log"
    private const val MAX_RECORDS = 500

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Volatile
    private var sp: SharedPreferences? = null

    @Serializable
    data class ApiLogRecord(
        val id: String = UUID.randomUUID().toString(),
        val timestamp: Long = System.currentTimeMillis(),
        val category: String,
        val vendor: String = "",
        val model: String = "",
        val source: String = "",
        val protocol: String = "HTTP",
        val method: String = "",
        val host: String = "",
        val path: String = "",
        val queryKeys: List<String> = emptyList(),
        val requestSummary: String = "",
        val requestStructure: String = "",
        val responseSummary: String = "",
        val httpCode: Int = 0,
        val success: Boolean = false,
        val canceled: Boolean = false,
        val durationMs: Long = 0,
        val errorSummary: String = ""
    )

    fun initialize(context: Context) {
        if (sp != null) return
        synchronized(this) {
            if (sp == null) {
                sp = context.applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    fun add(record: ApiLogRecord) {
        val prefs = sp ?: return
        synchronized(this) {
            val list = readAllInternal(prefs)
            list.add(record)
            val pruned = list.sortedByDescending { it.timestamp }.take(MAX_RECORDS)
            writeAllInternal(prefs, pruned)
        }
    }

    fun listAll(): List<ApiLogRecord> {
        val prefs = sp ?: return emptyList()
        return synchronized(this) {
            readAllInternal(prefs).sortedByDescending { it.timestamp }
        }
    }

    fun clearAll() {
        val prefs = sp ?: return
        synchronized(this) {
            prefs.edit().remove(KEY_API_LOG_JSON).apply()
        }
    }

    private fun readAllInternal(prefs: SharedPreferences): MutableList<ApiLogRecord> {
        val raw = prefs.getString(KEY_API_LOG_JSON, "").orEmpty()
        if (raw.isBlank()) return mutableListOf()
        return try {
            json.decodeFromString<List<ApiLogRecord>>(raw).toMutableList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse API log JSON", e)
            mutableListOf()
        }
    }

    private fun writeAllInternal(prefs: SharedPreferences, list: List<ApiLogRecord>) {
        try {
            prefs.edit().putString(KEY_API_LOG_JSON, json.encodeToString(list)).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write API log JSON", e)
        }
    }
}
