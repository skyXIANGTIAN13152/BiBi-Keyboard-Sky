/**
 * Pro 相关 Compose 弹窗共享组件。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.LocalSettingsHapticTap
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import com.brycewg.asrkb.ui.settings.compose.core.settingsDialogShape
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ProDialogSurface(
    uiMode: BibiUiMode,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (uiMode) {
        BibiUiMode.Material -> Surface(
            modifier = modifier.fillMaxWidth(),
            shape = settingsDialogShape(),
            tonalElevation = SettingsLayoutMetrics.ProDialogElevation,
            color = MaterialTheme.colorScheme.surface,
            content = content
        )

        BibiUiMode.Miuix -> Column(modifier = modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
internal fun Modifier.proDialogScrollableContent(): Modifier {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.82f
    return fillMaxWidth()
        .heightIn(max = maxHeight)
        .verticalScroll(rememberScrollState())
}

@Composable
internal fun DialogTitle(text: String, uiMode: BibiUiMode) {
    when (uiMode) {
        BibiUiMode.Material -> MaterialText(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        BibiUiMode.Miuix -> MiuixText(
            text = text,
            fontSize = MiuixTheme.textStyles.title2.fontSize,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun DialogSectionLabel(text: String, uiMode: BibiUiMode) {
    when (uiMode) {
        BibiUiMode.Material -> MaterialText(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        BibiUiMode.Miuix -> MiuixText(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun DialogBody(
    text: String,
    uiMode: BibiUiMode,
    primary: Boolean = false,
    textAlign: TextAlign? = null
) {
    when (uiMode) {
        BibiUiMode.Material -> MaterialText(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = textAlign
        )

        BibiUiMode.Miuix -> MiuixText(
            text = text,
            color = if (primary) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
            textAlign = textAlign
        )
    }
}

@Composable
internal fun DialogCaption(
    text: String,
    uiMode: BibiUiMode,
    textAlign: TextAlign? = null
) {
    when (uiMode) {
        BibiUiMode.Material -> MaterialText(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign
        )

        BibiUiMode.Miuix -> MiuixText(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = textAlign
        )
    }
}

@Composable
internal fun DialogPrimaryAction(
    text: String,
    uiMode: BibiUiMode,
    onClick: () -> Unit
) {
    val clickWithHaptic = dialogClickWithHaptic(onClick)
    when (uiMode) {
        BibiUiMode.Material -> Button(
            onClick = clickWithHaptic,
            modifier = Modifier.fillMaxWidth()
        ) {
            MaterialText(text)
        }

        BibiUiMode.Miuix -> MiuixButton(
            onClick = clickWithHaptic,
            modifier = Modifier.fillMaxWidth(),
            colors = MiuixButtonDefaults.buttonColorsPrimary()
        ) {
            MiuixText(text)
        }
    }
}

@Composable
internal fun DialogTonalAction(
    text: String,
    uiMode: BibiUiMode,
    onClick: () -> Unit
) {
    val clickWithHaptic = dialogClickWithHaptic(onClick)
    when (uiMode) {
        BibiUiMode.Material -> Button(
            onClick = clickWithHaptic,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors()
        ) {
            MaterialText(text)
        }

        BibiUiMode.Miuix -> MiuixButton(
            onClick = clickWithHaptic,
            modifier = Modifier.fillMaxWidth()
        ) {
            MiuixText(text)
        }
    }
}

@Composable
internal fun DialogTextAction(
    text: String,
    uiMode: BibiUiMode,
    onClick: () -> Unit
) {
    val clickWithHaptic = dialogClickWithHaptic(onClick)
    when (uiMode) {
        BibiUiMode.Material -> MaterialTextButton(
            onClick = clickWithHaptic,
            modifier = Modifier.fillMaxWidth()
        ) {
            MaterialText(text)
        }

        BibiUiMode.Miuix -> MiuixTextButton(
            text = text,
            onClick = clickWithHaptic,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun dialogClickWithHaptic(onClick: () -> Unit): () -> Unit {
    val hapticTap = LocalSettingsHapticTap.current
    return {
        hapticTap()
        onClick()
    }
}
