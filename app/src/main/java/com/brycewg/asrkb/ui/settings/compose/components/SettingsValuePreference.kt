/**
 * Compose 设置页点击型值展示项，统一 Material 与 Miuix 外观。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.ListItemDefaults as MaterialListItemDefaults
import androidx.compose.material3.MaterialTheme as MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.LocalSettingsHapticTap
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.preference.ArrowPreference as MiuixArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsValuePreference(
    @StringRes titleRes: Int,
    value: String,
    uiMode: BibiUiMode,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
    trailingActionIcon: ImageVector? = null,
    @StringRes trailingActionContentDescriptionRes: Int? = null,
    onTrailingActionClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val hapticTap = LocalSettingsHapticTap.current
    val clickWithHaptic = {
        hapticTap()
        onClick()
    }
    val trailingClickWithHaptic = onTrailingActionClick?.let { action ->
        {
            hapticTap()
            action()
        }
    }
    when (uiMode) {
        BibiUiMode.Material -> SettingsMaterialItemSurface(
            index = index,
            count = count,
            modifier = modifier.clickable(role = Role.Button, onClick = clickWithHaptic)
        ) {
            val materialTrailingContent: (@Composable () -> Unit)? =
                if (trailingActionIcon != null && trailingClickWithHaptic != null) {
                    {
                        MaterialIconButton(onClick = trailingClickWithHaptic) {
                            MaterialIcon(
                                imageVector = trailingActionIcon,
                                contentDescription = trailingActionContentDescriptionRes?.let { stringResource(it) },
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    null
                }
            MaterialListItem(
                modifier = Modifier.heightIn(min = SettingsLayoutMetrics.SettingsPreferenceMinHeight),
                headlineContent = { MaterialText(stringResource(titleRes)) },
                supportingContent = { MaterialText(value) },
                trailingContent = materialTrailingContent,
                colors = MaterialListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        BibiUiMode.Miuix -> MiuixArrowPreference(
            title = stringResource(titleRes),
            summary = value,
            endActions = {
                if (trailingActionIcon != null && trailingClickWithHaptic != null) {
                    MiuixIconButton(
                        onClick = trailingClickWithHaptic,
                        modifier = Modifier.size(36.dp),
                        minWidth = 36.dp,
                        minHeight = 36.dp
                    ) {
                        MiuixIcon(
                            imageVector = trailingActionIcon,
                            contentDescription = trailingActionContentDescriptionRes?.let { stringResource(it) },
                            modifier = Modifier.size(18.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }
                }
            },
            onClick = clickWithHaptic
        )
    }
}
