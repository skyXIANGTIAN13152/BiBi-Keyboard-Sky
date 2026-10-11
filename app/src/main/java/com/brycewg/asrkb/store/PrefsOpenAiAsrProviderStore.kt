/**
 * OpenAI ASR 渠道配置列表的序列化与旧字段迁移。
 *
 * 归属模块：store
 */
package com.brycewg.asrkb.store

import android.util.Log
import com.brycewg.asrkb.R
import kotlinx.serialization.json.Json

internal object PrefsOpenAiAsrProviderStore {
    private const val TAG = "Prefs"

    fun getOpenAiAsrProviders(prefs: Prefs, json: Json): List<Prefs.OpenAiAsrProvider> {
        if (prefs.openAiAsrProvidersJson.isBlank()) {
            val migrated = prefs.buildLegacyOpenAiAsrProvider(
                name = prefs.getLocalizedString(R.string.openai_profile_default_name, 1)
            )
            setOpenAiAsrProviders(prefs, json, listOf(migrated))
        }
        return try {
            json.decodeFromString<List<Prefs.OpenAiAsrProvider>>(prefs.openAiAsrProvidersJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse OpenAI ASR providers JSON", e)
            emptyList()
        }
    }

    fun setOpenAiAsrProviders(prefs: Prefs, json: Json, list: List<Prefs.OpenAiAsrProvider>) {
        try {
            prefs.openAiAsrProvidersJson = json.encodeToString(list)
            if (list.none { it.id == prefs.activeOpenAiAsrProviderId }) {
                prefs.activeOpenAiAsrProviderId = list.firstOrNull()?.id ?: ""
            }
            prefs.syncLegacyOpenAiAsrFields(getActiveOpenAiAsrProvider(prefs, json))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize OpenAI ASR providers", e)
        }
    }

    fun getActiveOpenAiAsrProvider(prefs: Prefs, json: Json): Prefs.OpenAiAsrProvider? {
        val id = prefs.activeOpenAiAsrProviderId
        val list = getOpenAiAsrProviders(prefs, json)
        return list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }
}
