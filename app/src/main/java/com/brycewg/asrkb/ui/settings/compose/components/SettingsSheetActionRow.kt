/**
 * Compose 设置底部弹层操作按钮行，统一 Material 与 Miuix 的 action 布局。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.LocalSettingsHapticTap
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

@Composable
internal fun SettingsSheetActionRow(
    uiMode: BibiUiMode,
    cancelText: String,
    confirmText: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SettingsLayoutMetrics.SheetHorizontalPadding,
        vertical = SettingsLayoutMetrics.SheetBottomPadding
    )
) {
    val hapticTap = LocalSettingsHapticTap.current
    val dismissWithHaptic = {
        hapticTap()
        onDismiss()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.SheetActionButtonSpacing)
    ) {
        when (uiMode) {
            BibiUiMode.Material -> TextButton(
                onClick = dismissWithHaptic,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = SettingsLayoutMetrics.ActionButtonMinHeight),
                shape = RoundedCornerShape(SettingsLayoutMetrics.ActionButtonCorner),
                contentPadding = PaddingValues(
                    horizontal = SettingsLayoutMetrics.ActionButtonInsideHorizontalPadding,
                    vertical = SettingsLayoutMetrics.ActionButtonInsideVerticalPadding
                )
            ) {
                Text(
                    text = cancelText,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }

            BibiUiMode.Miuix -> MiuixTextButton(
                text = cancelText,
                onClick = dismissWithHaptic,
                modifier = Modifier.weight(1f)
            )
        }
        SettingsActionButton(
            uiMode = uiMode,
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f)
        )
    }
}
