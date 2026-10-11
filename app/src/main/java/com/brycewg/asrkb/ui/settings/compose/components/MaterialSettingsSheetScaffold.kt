/**
 * Material 风格设置底部弹层公共外壳。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import com.brycewg.asrkb.ui.settings.compose.core.settingsModalSheetShape
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun MaterialSettingsSheetScaffold(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = SettingsLayoutMetrics.SheetBottomPadding,
    content: @Composable ColumnScope.(dismissWithAnimation: (afterDismiss: () -> Unit) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var dismissing by remember { mutableStateOf(false) }

    fun dismissWithAnimation(afterDismiss: () -> Unit = {}) {
        if (dismissing) return
        dismissing = true
        scope.launch {
            sheetState.hide()
            afterDismiss()
            onDismiss()
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { dismissWithAnimation() },
        shape = settingsModalSheetShape()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsLayoutMetrics.SheetHorizontalPadding)
                .padding(bottom = bottomPadding)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(SettingsLayoutMetrics.SheetTitleBottomPadding))
            }
            content(::dismissWithAnimation)
        }
    }
}
