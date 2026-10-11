/**
 * 新手引导 Compose 页面主体。
 *
 * 归属模块：ui/setup/compose
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.setup.compose

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ui.settings.compose.components.SettingsDetailScaffold
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import com.brycewg.asrkb.ui.settings.compose.core.SettingsMotion
import kotlin.math.abs
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val PAGE_COUNT = 4
private const val LAST_PAGE_INDEX = PAGE_COUNT - 1

@Composable
internal fun OnboardingGuideScreen(
    uiMode: BibiUiMode,
    refreshKey: Int,
    permissionGroups: List<OnboardingPermissionGroup>,
    asrChoice: OnboardingAsrChoice,
    dataCollectionEnabled: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNextFromLastPage: () -> Unit,
    onAsrChoiceChange: (OnboardingAsrChoice) -> Unit,
    onDataCollectionChange: (Boolean) -> Unit,
    onOpenProject: () -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenDocs: () -> Unit,
    onOpenPro: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    OnboardingScaffold(
        uiMode = uiMode,
        currentPage = pagerState.currentPage,
        onBack = onBack,
        onSkip = onSkip,
        onPrevious = {
            scope.launch {
                pagerState.animateToOnboardingPage((pagerState.currentPage - 1).coerceAtLeast(0))
            }
        },
        onNext = {
            if (pagerState.currentPage >= LAST_PAGE_INDEX) {
                onNextFromLastPage()
            } else {
                scope.launch {
                    pagerState.animateToOnboardingPage(pagerState.currentPage + 1)
                }
            }
        }
    ) { innerPadding, scrollModifier ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection) + SettingsLayoutMetrics.PageHorizontalPadding,
            top = SettingsLayoutMetrics.PageVerticalPadding,
            end = innerPadding.calculateEndPadding(layoutDirection) + SettingsLayoutMetrics.PageHorizontalPadding,
            bottom = SettingsLayoutMetrics.PageVerticalPadding
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> key(refreshKey) {
                    OnboardingPermissionsPage(
                        groups = permissionGroups,
                        uiMode = uiMode,
                        modifier = scrollModifier,
                        contentPadding = contentPadding
                    )
                }

                1 -> OnboardingAsrChoicePage(
                    selected = asrChoice,
                    uiMode = uiMode,
                    modifier = scrollModifier,
                    contentPadding = contentPadding,
                    onSelected = onAsrChoiceChange
                )

                2 -> OnboardingPrivacyPage(
                    checked = dataCollectionEnabled,
                    uiMode = uiMode,
                    modifier = scrollModifier,
                    contentPadding = contentPadding,
                    onCheckedChange = onDataCollectionChange
                )

                3 -> OnboardingLinksPage(
                    uiMode = uiMode,
                    modifier = scrollModifier,
                    contentPadding = contentPadding,
                    onOpenProject = onOpenProject,
                    onOpenWebsite = onOpenWebsite,
                    onOpenDocs = onOpenDocs,
                    onOpenPro = onOpenPro
                )
            }
        }
    }
}

@Composable
private fun OnboardingScaffold(
    uiMode: BibiUiMode,
    currentPage: Int,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    content: @Composable (PaddingValues, Modifier) -> Unit
) {
    SettingsDetailScaffold(
        uiMode = uiMode,
        titleRes = R.string.onboarding_title,
        onBack = onBack,
        content = { innerPadding, scrollModifier ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    content(innerPadding, scrollModifier)
                }
                OnboardingBottomBar(
                    uiMode = uiMode,
                    currentPage = currentPage,
                    onSkip = onSkip,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                    )
                )
            }
        }
    )
}

@Composable
private fun OnboardingBottomBar(
    uiMode: BibiUiMode,
    currentPage: Int,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        OnboardingProgressIndicator(
            uiMode = uiMode,
            progress = (currentPage + 1) / PAGE_COUNT.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )
        PageIndicator(
            text = stringResource(R.string.onboarding_page_indicator, currentPage + 1, PAGE_COUNT),
            uiMode = uiMode
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            OnboardingTextAction(
                text = stringResource(R.string.onboarding_btn_skip),
                uiMode = uiMode,
                visible = currentPage < LAST_PAGE_INDEX,
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            )
            OnboardingTextAction(
                text = stringResource(R.string.onboarding_btn_prev),
                uiMode = uiMode,
                enabled = currentPage > 0,
                onClick = onPrevious,
                modifier = Modifier.weight(1f)
            )
            OnboardingPrimaryAction(
                text = stringResource(
                    if (currentPage >= LAST_PAGE_INDEX) {
                        R.string.onboarding_btn_finish
                    } else {
                        R.string.onboarding_btn_next
                    }
                ),
                uiMode = uiMode,
                onClick = onNext,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OnboardingProgressIndicator(
    uiMode: BibiUiMode,
    progress: Float,
    modifier: Modifier = Modifier
) {
    when (uiMode) {
        BibiUiMode.Material -> LinearProgressIndicator(
            progress = { progress },
            modifier = modifier
        )

        BibiUiMode.Miuix -> MiuixLinearProgressIndicator(
            progress = progress,
            modifier = modifier
        )
    }
}

@Composable
private fun PageIndicator(text: String, uiMode: BibiUiMode) {
    when (uiMode) {
        BibiUiMode.Material -> Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )

        BibiUiMode.Miuix -> MiuixText(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1
        )
    }
}

@Composable
private fun OnboardingTextAction(
    text: String,
    uiMode: BibiUiMode,
    visible: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) {
        Row(modifier = modifier) {}
        return
    }
    when (uiMode) {
        BibiUiMode.Material -> TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) {
            Text(text)
        }

        BibiUiMode.Miuix -> MiuixTextButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        )
    }
}

@Composable
private fun OnboardingPrimaryAction(
    text: String,
    uiMode: BibiUiMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiMode) {
        BibiUiMode.Material -> Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text)
        }

        BibiUiMode.Miuix -> MiuixTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            colors = MiuixButtonDefaults.textButtonColorsPrimary()
        )
    }
}

private suspend fun PagerState.animateToOnboardingPage(targetPage: Int) {
    val distance = abs(targetPage - currentPage).coerceAtLeast(SettingsMotion.PAGER_MINIMUM_DISTANCE_PAGES)
    val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
    val currentDistanceInPages = targetPage - currentPage - currentPageOffsetFraction
    animateScrollBy(
        value = currentDistanceInPages * pageSize,
        animationSpec = SettingsMotion.pagerSpec(distance)
    )
}
