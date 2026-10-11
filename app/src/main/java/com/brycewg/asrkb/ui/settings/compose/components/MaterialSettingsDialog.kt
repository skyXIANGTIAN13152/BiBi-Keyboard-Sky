/**
 * Material 风格设置弹窗公共外壳。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import com.brycewg.asrkb.ui.settings.compose.core.settingsDialogShape

internal data class MaterialSettingsDialogAction(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val primary: Boolean = false
)

@Composable
internal fun MaterialSettingsAlertDialog(
    title: String? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    text: @Composable (() -> Unit)? = null,
    titleContent: @Composable (() -> Unit)? = title?.let { { Text(it) } },
    buttons: @Composable () -> Unit
) {
    AlertDialog(
        modifier = modifier.widthIn(max = SettingsLayoutMetrics.DialogMaxWidth),
        shape = settingsDialogShape(),
        onDismissRequest = onDismissRequest,
        title = titleContent,
        text = text,
        confirmButton = buttons
    )
}

@Composable
internal fun MaterialSettingsDialogButtonRow(
    actions: List<MaterialSettingsDialogAction>
) {
    val hasPrimaryAction = actions.any { it.primary }
    SettingsDialogActionRow(
        uiMode = BibiUiMode.Material,
        actions = actions.mapIndexed { index, action ->
            SettingsDialogAction(
                text = action.text,
                onClick = action.onClick,
                enabled = action.enabled,
                primary = action.primary || (!hasPrimaryAction && index == actions.lastIndex)
            )
        }
    )
}
