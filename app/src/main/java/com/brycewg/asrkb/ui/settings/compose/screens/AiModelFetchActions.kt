/**
 * Compose AI 设置页模型拉取动作。
 *
 * 归属模块：ui/settings/compose/screens
 */
package com.brycewg.asrkb.ui.settings.compose.screens

import android.content.Context
import android.util.Log
import com.brycewg.asrkb.BuildConfig
import com.brycewg.asrkb.R
import com.brycewg.asrkb.asr.LlmPostProcessor
import com.brycewg.asrkb.ui.settings.compose.components.SettingsProgressDialogState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AI_MODEL_FETCH_TAG = "AiModelFetchActions"

internal fun launchAiModelsFetch(
    context: Context,
    coroutineScope: CoroutineScope,
    endpoint: String,
    apiKey: String,
    flushPendingWrites: () -> Unit,
    onMissingParams: () -> Unit,
    onProgressChange: (SettingsProgressDialogState?) -> Unit,
    onClearProgress: (SettingsProgressDialogState) -> Unit,
    onFetchFailed: (String) -> Unit,
    onSuccess: (List<String>) -> Unit
) {
    flushPendingWrites()
    if (endpoint.isBlank()) {
        onMissingParams()
        return
    }

    val progressState = SettingsProgressDialogState(
        message = context.getString(R.string.llm_models_fetching)
    )
    onProgressChange(progressState)
    coroutineScope.launch {
        try {
            val result = withContext(Dispatchers.IO) {
                LlmPostProcessor().fetchModels(endpoint, apiKey)
            }
            if (result.ok) {
                onSuccess(result.models)
            } else {
                onFetchFailed(result.message ?: context.getString(R.string.llm_test_failed_generic))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.d(AI_MODEL_FETCH_TAG, "Failed to fetch LLM models", e)
            onFetchFailed(e.message ?: context.getString(R.string.llm_test_failed_generic))
        } finally {
            onClearProgress(progressState)
        }
    }
}
