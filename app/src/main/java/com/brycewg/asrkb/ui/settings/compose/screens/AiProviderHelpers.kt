/**
 * Compose AI 设置页供应商表单的模型与数值辅助函数。
 *
 * 归属模块：ui/settings/compose/screens
 */

package com.brycewg.asrkb.ui.settings.compose.screens

import com.brycewg.asrkb.asr.LlmVendor
import com.brycewg.asrkb.store.Prefs
import java.util.Locale

internal fun currentSfModel(prefs: Prefs): String = if (prefs.sfFreeLlmUsePaidKey) {
    prefs.getLlmVendorModel(LlmVendor.SF_FREE).ifBlank { prefs.sfFreeLlmModel }
} else {
    prefs.sfFreeLlmModel
}

internal fun getSfPresetModels(prefs: Prefs): List<String> = if (prefs.sfFreeLlmUsePaidKey) {
    prefs.getLlmVendorModels(LlmVendor.SF_FREE)
} else {
    Prefs.SF_FREE_LLM_MODELS
}

internal fun getSfStaticModels(prefs: Prefs): List<String> = if (prefs.sfFreeLlmUsePaidKey) {
    LlmVendor.SF_FREE.models
} else {
    Prefs.SF_FREE_LLM_MODELS
}

internal data class AiBuiltinModelsSelectionResult(
    val sfPresetModels: List<String>? = null,
    val sfStaticModels: List<String>? = null,
    val builtinPresetModels: List<String>? = null,
    val sfModel: String? = null,
    val builtinModel: String? = null
)

internal fun applyAiBuiltinModelsSelection(
    prefs: Prefs,
    vendor: LlmVendor,
    activeVendor: LlmVendor,
    selectedModels: List<String>
): AiBuiltinModelsSelectionResult {
    prefs.setLlmVendorModels(vendor, selectedModels)

    val currentModel = if (vendor == LlmVendor.SF_FREE && !prefs.sfFreeLlmUsePaidKey) {
        prefs.sfFreeLlmModel
    } else {
        prefs.getLlmVendorModel(vendor)
    }
    val nextModel = nextSelectedModel(currentModel, selectedModels)
    if (vendor == LlmVendor.SF_FREE && !prefs.sfFreeLlmUsePaidKey) {
        prefs.sfFreeLlmModel = nextModel
        return AiBuiltinModelsSelectionResult(
            sfPresetModels = getSfPresetModels(prefs),
            sfStaticModels = getSfStaticModels(prefs),
            sfModel = nextModel
        )
    }

    prefs.setLlmVendorModel(vendor, nextModel)
    return AiBuiltinModelsSelectionResult(
        sfPresetModels = if (vendor == LlmVendor.SF_FREE) getSfPresetModels(prefs) else null,
        sfStaticModels = if (vendor == LlmVendor.SF_FREE) getSfStaticModels(prefs) else null,
        builtinPresetModels = if (vendor == activeVendor) selectedModels else null,
        sfModel = if (vendor == LlmVendor.SF_FREE) nextModel else null,
        builtinModel = if (vendor != LlmVendor.SF_FREE) nextModel else null
    )
}

internal fun customProviderModels(provider: Prefs.LlmProvider?): List<String> = provider?.models.orEmpty().map { it.trim() }.filter { it.isNotBlank() }

internal fun nextSelectedModel(currentModel: String, selectedModels: List<String>): String = currentModel.takeIf { it.isNotBlank() && selectedModels.contains(it) } ?: selectedModels.first()

internal fun formatTemperature(value: Float): String = String.format(Locale.US, "%.1f", value)

internal fun temperatureSteps(vendor: LlmVendor): Int {
    val span = (vendor.temperatureMax - vendor.temperatureMin).coerceAtLeast(0f)
    return ((span / 0.1f).toInt() - 1).coerceAtLeast(0)
}
