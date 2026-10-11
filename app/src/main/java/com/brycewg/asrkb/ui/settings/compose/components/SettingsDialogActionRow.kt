/**
 * Compose 设置弹窗操作按钮行，统一 Material 与 Miuix 的 action 布局。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.LocalSettingsHapticTap
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

internal data class SettingsDialogAction(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val primary: Boolean = false
)

@Composable
internal fun SettingsDialogActionRow(
    uiMode: BibiUiMode,
    actions: List<SettingsDialogAction>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    spacing: Dp = SettingsLayoutMetrics.DialogActionButtonSpacing
) {
    if (actions.isEmpty()) return
    val rowModifier = modifier
        .fillMaxWidth()
        .padding(contentPadding)
    if (actions.size > 2) {
        Column(
            modifier = rowModifier,
            verticalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.ActionButtonSpacing)
        ) {
            actions.forEach { action ->
                SettingsDialogActionButton(
                    uiMode = uiMode,
                    action = action,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        actions.forEach { action ->
            SettingsDialogActionButton(
                uiMode = uiMode,
                action = action,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingsDialogActionButton(
    uiMode: BibiUiMode,
    action: SettingsDialogAction,
    modifier: Modifier
) {
    val hapticTap = LocalSettingsHapticTap.current
    val clickWithHaptic = {
        hapticTap()
        action.onClick()
    }
    when (uiMode) {
        BibiUiMode.Material -> MaterialDialogActionButton(action, modifier, clickWithHaptic)

        BibiUiMode.Miuix -> MiuixTextButton(
            text = action.text,
            onClick = clickWithHaptic,
            enabled = action.enabled,
            modifier = modifier,
            colors = if (action.primary) {
                MiuixButtonDefaults.textButtonColorsPrimary()
            } else {
                MiuixButtonDefaults.textButtonColors()
            }
        )
    }
}

@Composable
private fun MaterialDialogActionButton(
    action: SettingsDialogAction,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val buttonModifier = modifier.heightIn(min = SettingsLayoutMetrics.ActionButtonMinHeight)
    val shape = RoundedCornerShape(SettingsLayoutMetrics.ActionButtonCorner)
    val contentPadding = PaddingValues(
        horizontal = SettingsLayoutMetrics.ActionButtonInsideHorizontalPadding,
        vertical = SettingsLayoutMetrics.ActionButtonInsideVerticalPadding
    )
    val content: @Composable () -> Unit = {
        Text(
            text = action.text,
            color = if (!action.enabled && !action.primary) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            } else {
                Color.Unspecified
            },
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
    if (action.primary) {
        Button(
            onClick = onClick,
            enabled = action.enabled,
            modifier = buttonModifier,
            shape = shape,
            contentPadding = contentPadding
        ) {
            content()
        }
    } else {
        TextButton(
            onClick = onClick,
            enabled = action.enabled,
            modifier = buttonModifier,
            shape = shape,
            contentPadding = contentPadding
        ) {
            content()
        }
    }
}
