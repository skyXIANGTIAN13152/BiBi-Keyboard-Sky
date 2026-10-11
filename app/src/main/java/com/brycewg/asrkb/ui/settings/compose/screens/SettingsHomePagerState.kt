/**
 * 设置首页 Tab/Pager 动画状态。
 *
 * 归属模块：ui/settings/compose/screens
 */
package com.brycewg.asrkb.ui.settings.compose.screens

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.brycewg.asrkb.ui.settings.compose.core.SettingsMotion
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class SettingsHomePagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetPage: Int) {
        if (targetPage == selectedPage) return

        navJob?.cancel()
        selectedPage = targetPage
        isNavigating = true

        val distance = abs(targetPage - pagerState.currentPage)
            .coerceAtLeast(SettingsMotion.PAGER_MINIMUM_DISTANCE_PAGES)
        val layoutInfo = pagerState.layoutInfo
        val pageSize = layoutInfo.pageSize + layoutInfo.pageSpacing
        val currentDistanceInPages =
            targetPage - pagerState.currentPage - pagerState.currentPageOffsetFraction
        val scrollPixels = currentDistanceInPages * pageSize

        navJob = coroutineScope.launch {
            val currentJob = coroutineContext.job
            try {
                pagerState.animateScrollBy(
                    value = scrollPixels,
                    animationSpec = SettingsMotion.pagerSpec(distance)
                )
            } finally {
                if (navJob == currentJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetPage) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
internal fun rememberSettingsHomePagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): SettingsHomePagerState = remember(pagerState, coroutineScope) {
    SettingsHomePagerState(pagerState, coroutineScope)
}
