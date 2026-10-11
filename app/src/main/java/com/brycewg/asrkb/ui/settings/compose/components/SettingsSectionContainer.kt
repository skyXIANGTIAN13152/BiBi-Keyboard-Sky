/**
 * Compose 设置分区容器，统一 Material 与 Miuix 的标题、间距和卡片布局。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsSectionContainer(
    uiMode: BibiUiMode,
    @StringRes titleRes: Int? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        titleRes?.let { SettingsSectionTitle(stringResource(it), uiMode) }
        when (uiMode) {
            BibiUiMode.Material -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.MaterialSectionItemSpacing),
                content = content
            )

            BibiUiMode.Miuix -> MiuixCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(vertical = SettingsLayoutMetrics.SectionContainerVerticalPadding),
                    content = content
                )
            }
        }
    }
}

@Composable
internal fun SettingsSectionTitle(text: String, uiMode: BibiUiMode) {
    when (uiMode) {
        BibiUiMode.Material -> Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                horizontal = SettingsLayoutMetrics.DetailSectionTitleHorizontalPadding,
                vertical = SettingsLayoutMetrics.DetailSectionTitleVerticalPadding
            )
        )

        BibiUiMode.Miuix -> MiuixText(
            text = text,
            modifier = Modifier.padding(
                horizontal = SettingsLayoutMetrics.DetailSectionTitleHorizontalPadding,
                vertical = SettingsLayoutMetrics.DetailSectionTitleVerticalPadding
            ),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1
        )
    }
}
