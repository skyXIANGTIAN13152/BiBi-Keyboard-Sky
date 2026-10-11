/**
 * Compose 测试输入底部面板。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

@Composable
internal fun SettingsTestInputSheet(
    show: Boolean,
    uiMode: BibiUiMode,
    onDismiss: () -> Unit
) {
    if (!show) return
    when (uiMode) {
        BibiUiMode.Material -> MaterialTestInputSheet(onDismiss)
        BibiUiMode.Miuix -> MiuixTestInputSheet(onDismiss)
    }
}

@Composable
private fun MaterialTestInputSheet(onDismiss: () -> Unit) {
    MaterialSettingsSheetScaffold(
        title = stringResource(R.string.label_test_input),
        onDismiss = onDismiss
    ) { _ ->
        TestInputTextField(uiMode = BibiUiMode.Material)
    }
}

@Composable
private fun MiuixTestInputSheet(onDismiss: () -> Unit) {
    var show by remember { mutableStateOf(true) }
    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.label_test_input),
        onDismissRequest = { show = false },
        onDismissFinished = onDismiss
    ) {
        TestInputTextField(
            uiMode = BibiUiMode.Miuix,
            modifier = Modifier.padding(bottom = SettingsLayoutMetrics.SheetBottomPadding)
        )
    }
}

@Composable
private fun TestInputTextField(
    uiMode: BibiUiMode,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val label = stringResource(R.string.label_test_input)

    LaunchedEffect(Unit) {
        delay(160)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val fieldModifier = modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)

    SettingsTextField(
        uiMode = uiMode,
        value = text,
        onValueChange = { text = it },
        label = label,
        modifier = fieldModifier,
        singleLine = false,
        minLines = 3,
        maxLines = 8,
        keyboardOptions = keyboardOptions,
        contentPadding = PaddingValues(0.dp)
    )
}
