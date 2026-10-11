/**
 * Material 设置项背景容器，用于补齐非标准 Preference 控件的分区外观。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import com.brycewg.asrkb.ui.settings.compose.core.settingsSegmentedItemShape

@Composable
internal fun SettingsMaterialItemSurface(
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
    shape: Shape = settingsSegmentedItemShape(index, count),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(SettingsLayoutMetrics.MaterialSectionElevation),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(content = { content() })
    }
}
