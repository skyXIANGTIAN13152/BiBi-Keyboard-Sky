/**
 * 设置首页三 Tab 的脚手架、列表与分区组件。
 *
 * 归属模块：ui/settings/compose/screens
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ui.settings.compose.components.SettingsLazyColumn
import com.brycewg.asrkb.ui.settings.compose.components.SettingsPreference
import com.brycewg.asrkb.ui.settings.compose.components.SettingsSectionContainer
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.LocalSettingsHapticTap
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import com.brycewg.asrkb.ui.settings.compose.model.SettingsSection
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

@Composable
internal fun SettingsHomeScaffold(
    uiMode: BibiUiMode,
    tabs: List<SettingsHomeTab>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    content: @Composable (PaddingValues, Modifier) -> Unit
) {
    val hapticTap = LocalSettingsHapticTap.current
    val selectTabWithHaptic = { index: Int ->
        hapticTap()
        onSelectTab(index)
    }
    when (uiMode) {
        BibiUiMode.Material -> MaterialHomeScaffold(tabs, selectedTab, selectTabWithHaptic, content)
        BibiUiMode.Miuix -> MiuixHomeScaffold(tabs, selectedTab, selectTabWithHaptic, content)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MaterialHomeScaffold(
    tabs: List<SettingsHomeTab>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    content: @Composable (PaddingValues, Modifier) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                windowInsets = TopAppBarDefaults.windowInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        SettingsLayoutMetrics.BottomBarElevation
                    )
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            val bottomBarInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = SettingsLayoutMetrics.BottomBarTopCorner,
                    topEnd = SettingsLayoutMetrics.BottomBarTopCorner
                ),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(
                    SettingsLayoutMetrics.BottomBarElevation
                ),
                tonalElevation = SettingsLayoutMetrics.BottomBarElevation
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SettingsLayoutMetrics.BottomBarMinHeight)
                        .windowInsetsPadding(bottomBarInsets),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = selectedTab == index
                        NavigationBarItem(
                            modifier = Modifier.weight(1f),
                            selected = selected,
                            onClick = { if (!selected) onSelectTab(index) },
                            icon = {
                                Icon(
                                    if (selected) tab.materialSelectedIcon else tab.materialUnselectedIcon,
                                    contentDescription = stringResource(tab.titleRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.titleRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        content = { innerPadding -> content(innerPadding, Modifier) }
    )
}

@Composable
private fun MiuixHomeScaffold(
    tabs: List<SettingsHomeTab>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    content: @Composable (PaddingValues, Modifier) -> Unit
) {
    val scrollBehavior = MiuixScrollBehavior()
    MiuixScaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.settings_title),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            MiuixNavigationBar {
                tabs.forEachIndexed { index, tab ->
                    MiuixNavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { if (selectedTab != index) onSelectTab(index) },
                        icon = tab.miuixIcon,
                        label = stringResource(tab.titleRes)
                    )
                }
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        content = { innerPadding ->
            content(
                innerPadding,
                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            )
        }
    )
}

@Composable
internal fun SettingsSectionList(
    sections: List<SettingsSection>,
    uiMode: BibiUiMode,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = SettingsLayoutMetrics.PageContentPadding
) {
    SettingsLazyColumn(
        uiMode = uiMode,
        modifier = Modifier.fillMaxSize(),
        miuixScrollModifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.SectionSpacing)
    ) {
        settingsSectionItems(sections, uiMode)
    }
}

private fun LazyListScope.settingsSectionItems(
    sections: List<SettingsSection>,
    uiMode: BibiUiMode
) {
    sections.forEach { section ->
        item(key = section.id) {
            SettingsHomeSection(section = section, uiMode = uiMode)
        }
    }
}

@Composable
private fun SettingsHomeSection(
    section: SettingsSection,
    uiMode: BibiUiMode
) {
    SettingsSectionContainer(uiMode = uiMode, titleRes = section.titleRes) {
        section.entries.forEachIndexed { index, entry ->
            key(entry.id) {
                SettingsPreference(
                    entry = entry,
                    index = index,
                    count = section.entries.size
                )
            }
        }
    }
}

internal data class SettingsHomeTab(
    val titleRes: Int,
    val miuixIcon: ImageVector,
    val materialSelectedIcon: ImageVector,
    val materialUnselectedIcon: ImageVector
)

internal fun settingsHomeTabs(): List<SettingsHomeTab> = listOf(
    SettingsHomeTab(
        titleRes = R.string.settings_tab_input,
        miuixIcon = Icons.Rounded.Keyboard,
        materialSelectedIcon = Icons.Filled.Keyboard,
        materialUnselectedIcon = Icons.Outlined.Keyboard
    ),
    SettingsHomeTab(
        titleRes = R.string.settings_tab_smart,
        miuixIcon = Icons.Rounded.AutoAwesome,
        materialSelectedIcon = Icons.Filled.AutoAwesome,
        materialUnselectedIcon = Icons.Outlined.AutoAwesome
    ),
    SettingsHomeTab(
        titleRes = R.string.settings_tab_system,
        miuixIcon = Icons.Rounded.Settings,
        materialSelectedIcon = Icons.Filled.Settings,
        materialUnselectedIcon = Icons.Outlined.Settings
    )
)
