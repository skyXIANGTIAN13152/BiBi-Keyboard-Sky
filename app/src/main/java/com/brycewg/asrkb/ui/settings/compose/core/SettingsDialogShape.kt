/**
 * 设置页弹窗形状。
 *
 * 归属模块：ui/settings/compose/core
 */
package com.brycewg.asrkb.ui.settings.compose.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

internal fun settingsDialogShape(): Shape = RoundedCornerShape(SettingsLayoutMetrics.DialogCorner)
