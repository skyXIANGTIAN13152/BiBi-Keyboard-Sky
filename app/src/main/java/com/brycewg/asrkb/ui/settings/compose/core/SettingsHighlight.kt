/**
 * 设置页搜索跳转目标高亮状态。
 *
 * 归属模块：ui/settings/compose/core
 */
package com.brycewg.asrkb.ui.settings.compose.core

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSettingsHighlightTarget = staticCompositionLocalOf<String?> { null }
