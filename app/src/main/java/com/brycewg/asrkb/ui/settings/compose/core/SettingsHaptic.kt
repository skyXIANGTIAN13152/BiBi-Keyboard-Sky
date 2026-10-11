/**
 * 设置页 Compose 触感反馈入口。
 *
 * 归属模块：ui/settings/compose/core
 */
package com.brycewg.asrkb.ui.settings.compose.core

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalSettingsHapticTap = staticCompositionLocalOf<() -> Unit> { {} }
