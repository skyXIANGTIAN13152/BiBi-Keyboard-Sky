/**
 * 设置页 Compose 主题桥接。
 *
 * 归属模块：ui/settings/compose/core
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.core

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun BibiSettingsTheme(
    uiMode: BibiUiMode,
    themeMode: String,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val materialColorScheme = bibiMaterialColorScheme(isDark)

    SyncSystemBarAppearance(isDark)

    CompositionLocalProvider(
        LocalBibiUiMode provides uiMode,
        LocalBibiSettingsDark provides isDark
    ) {
        when (uiMode) {
            BibiUiMode.Material -> MaterialTheme(
                colorScheme = materialColorScheme,
                shapes = bibiMaterialShapes,
                content = content
            )

            BibiUiMode.Miuix -> MaterialTheme(
                colorScheme = materialColorScheme,
                shapes = bibiMaterialShapes
            ) {
                val miuixColorSchemeMode = remember(themeMode) {
                    when (themeMode) {
                        "light" -> ColorSchemeMode.Light
                        "dark" -> ColorSchemeMode.Dark
                        else -> ColorSchemeMode.System
                    }
                }
                val miuixThemeController = remember(miuixColorSchemeMode, isDark) {
                    ThemeController(
                        miuixColorSchemeMode,
                        keyColor = null,
                        isDark = isDark
                    )
                }
                MiuixTheme(
                    controller = miuixThemeController
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MiuixTheme.colorScheme.onBackground
                    ) {
                        MiuixScaffold(
                            contentWindowInsets = WindowInsets(0.dp)
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun bibiMaterialColorScheme(isDark: Boolean): ColorScheme {
    val context = LocalContext.current
    return remember(context, isDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (isDark) darkColorScheme() else lightColorScheme()
        }
    }
}

private val bibiMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
private fun SyncSystemBarAppearance(isDark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return

    SideEffect {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }
}
