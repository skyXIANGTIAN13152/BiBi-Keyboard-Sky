/**
 * 设置页 Compose UI 模式定义。
 *
 * 归属模块：ui/settings/compose/core
 */
package com.brycewg.asrkb.ui.settings.compose.core

import androidx.compose.runtime.staticCompositionLocalOf

enum class BibiUiMode(val id: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        const val DEFAULT_ID = "miuix"

        fun fromId(id: String): BibiUiMode = when (id) {
            "material" -> Material
            else -> Miuix
        }
    }
}

val LocalBibiUiMode = staticCompositionLocalOf { BibiUiMode.Miuix }
val LocalBibiSettingsDark = staticCompositionLocalOf { false }
