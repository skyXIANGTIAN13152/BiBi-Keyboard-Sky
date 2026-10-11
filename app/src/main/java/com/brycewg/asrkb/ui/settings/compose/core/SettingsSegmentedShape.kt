/**
 * 设置页 Material 分段列表形状。
 *
 * 归属模块：ui/settings/compose/core
 */
package com.brycewg.asrkb.ui.settings.compose.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

internal fun settingsSegmentedItemShape(index: Int, count: Int): Shape {
    val itemCount = count.coerceAtLeast(1)
    val itemIndex = index.coerceIn(0, itemCount - 1)
    val outer = SettingsLayoutMetrics.MaterialSectionShape
    val inner = SettingsLayoutMetrics.MaterialSectionInnerShape
    return if (itemCount == 1) {
        RoundedCornerShape(outer)
    } else {
        when (itemIndex) {
            0 -> RoundedCornerShape(
                topStart = outer,
                topEnd = outer,
                bottomStart = inner,
                bottomEnd = inner
            )

            itemCount - 1 -> RoundedCornerShape(
                topStart = inner,
                topEnd = inner,
                bottomStart = outer,
                bottomEnd = outer
            )

            else -> RoundedCornerShape(inner)
        }
    }
}
