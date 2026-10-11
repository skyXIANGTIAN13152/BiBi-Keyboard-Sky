/**
 * 设置页列表容器，集中处理 Material 与 Miuix 的滚动差异。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
internal fun SettingsLazyColumn(
    uiMode: BibiUiMode,
    modifier: Modifier = Modifier,
    miuixScrollModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit
) {
    if (uiMode == BibiUiMode.Miuix) {
        LazyColumn(
            modifier = modifier
                .imePadding()
                .overScrollVertical()
                .then(miuixScrollModifier),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            overscrollEffect = null,
            content = content
        )
    } else {
        LazyColumn(
            modifier = modifier.imePadding(),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

@Composable
internal fun SettingsSheetLazyColumn(
    uiMode: BibiUiMode,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = SettingsLayoutMetrics.SheetBottomPadding),
    content: LazyListScope.() -> Unit
) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .heightIn(max = SettingsLayoutMetrics.SheetContentMaxHeight)
        .then(modifier)

    SettingsLazyColumn(
        uiMode = uiMode,
        modifier = baseModifier,
        contentPadding = contentPadding,
        content = content
    )
}
