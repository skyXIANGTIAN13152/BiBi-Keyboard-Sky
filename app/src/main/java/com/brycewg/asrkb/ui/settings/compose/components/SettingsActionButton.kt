/**
 * Compose 设置页操作按钮组件，统一 Material 与 Miuix 的按钮尺寸。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.LocalSettingsHapticTap
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsActionButtonRow(
    uiMode: BibiUiMode,
    modifier: Modifier = Modifier,
    padded: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val rowModifier = if (padded) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsLayoutMetrics.ActionButtonRowHorizontalPadding)
            .padding(
                top = SettingsLayoutMetrics.ActionButtonRowTopPadding,
                bottom = SettingsLayoutMetrics.ActionButtonRowBottomPadding
            )
    } else {
        modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.ActionButtonSpacing),
        content = content
    )
}

@Composable
internal fun SettingsActionButton(
    uiMode: BibiUiMode,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null
) {
    val buttonModifier = modifier.heightIn(min = SettingsLayoutMetrics.ActionButtonMinHeight)
    val hapticTap = LocalSettingsHapticTap.current
    val clickWithHaptic = {
        hapticTap()
        onClick()
    }
    when (uiMode) {
        BibiUiMode.Material -> Button(
            onClick = clickWithHaptic,
            enabled = enabled,
            modifier = buttonModifier,
            shape = RoundedCornerShape(SettingsLayoutMetrics.ActionButtonCorner),
            contentPadding = PaddingValues(
                horizontal = SettingsLayoutMetrics.ActionButtonInsideHorizontalPadding,
                vertical = SettingsLayoutMetrics.ActionButtonInsideVerticalPadding
            )
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.size(SettingsLayoutMetrics.ActionButtonIconSpacing))
            } else {
                leadingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(SettingsLayoutMetrics.ActionButtonIconSize)
                    )
                    Spacer(Modifier.size(SettingsLayoutMetrics.ActionButtonIconSpacing))
                }
            }
            Text(text)
        }

        BibiUiMode.Miuix -> MiuixButton(
            onClick = clickWithHaptic,
            enabled = enabled,
            modifier = buttonModifier,
            cornerRadius = SettingsLayoutMetrics.ActionButtonCorner,
            minHeight = SettingsLayoutMetrics.ActionButtonMinHeight,
            insideMargin = PaddingValues(
                horizontal = SettingsLayoutMetrics.ActionButtonInsideHorizontalPadding,
                vertical = SettingsLayoutMetrics.ActionButtonInsideVerticalPadding
            ),
            colors = MiuixButtonDefaults.buttonColorsPrimary()
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.size(SettingsLayoutMetrics.ActionButtonIconSpacing))
            } else {
                leadingIcon?.let {
                    MiuixIcon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(SettingsLayoutMetrics.ActionButtonIconSize)
                    )
                    Spacer(Modifier.size(SettingsLayoutMetrics.ActionButtonIconSpacing))
                }
            }
            MiuixText(text = text, style = MiuixTheme.textStyles.button)
        }
    }
}
