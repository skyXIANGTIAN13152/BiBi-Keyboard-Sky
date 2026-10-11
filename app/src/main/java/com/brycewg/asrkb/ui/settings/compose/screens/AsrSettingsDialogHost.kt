/**
 * ASR 设置页弹窗与底部弹层宿主。
 *
 * 归属模块：ui/settings/compose/screens
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.screens

import androidx.compose.runtime.Composable
import com.brycewg.asrkb.ui.settings.compose.components.SettingsChoiceSheet
import com.brycewg.asrkb.ui.settings.compose.components.SettingsChoiceSheetState
import com.brycewg.asrkb.ui.settings.compose.components.SettingsFeatureExplainerDialog
import com.brycewg.asrkb.ui.settings.compose.components.SettingsFeatureExplainerDialogState
import com.brycewg.asrkb.ui.settings.compose.components.SettingsMessageDialog
import com.brycewg.asrkb.ui.settings.compose.components.SettingsMessageDialogState
import com.brycewg.asrkb.ui.settings.compose.components.SettingsMultiChoiceSheet
import com.brycewg.asrkb.ui.settings.compose.components.SettingsMultiChoiceSheetState
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode

@Composable
internal fun AsrSettingsDialogHost(
    uiMode: BibiUiMode,
    choiceSheet: SettingsChoiceSheetState?,
    multiChoiceSheet: SettingsMultiChoiceSheetState?,
    featureExplainerDialog: SettingsFeatureExplainerDialogState?,
    messageDialog: SettingsMessageDialogState?,
    onDismissChoiceSheet: () -> Unit,
    onDismissMultiChoiceSheet: () -> Unit,
    onDismissFeatureExplainerDialog: () -> Unit,
    onDismissMessageDialog: () -> Unit
) {
    SettingsChoiceSheet(
        state = choiceSheet,
        uiMode = uiMode,
        onDismiss = onDismissChoiceSheet
    )
    SettingsMultiChoiceSheet(
        state = multiChoiceSheet,
        uiMode = uiMode,
        onDismiss = onDismissMultiChoiceSheet
    )
    SettingsFeatureExplainerDialog(
        state = featureExplainerDialog,
        uiMode = uiMode,
        onDismiss = onDismissFeatureExplainerDialog
    )
    SettingsMessageDialog(
        state = messageDialog,
        uiMode = uiMode,
        onDismiss = onDismissMessageDialog
    )
}
