/**
 * Compose Pro 付款码弹窗内容。
 *
 * 归属模块：ui/settings/compose/components
 */
@file:Suppress("FunctionName")

package com.brycewg.asrkb.ui.settings.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.brycewg.asrkb.R
import com.brycewg.asrkb.ui.settings.compose.core.BibiUiMode
import com.brycewg.asrkb.ui.settings.compose.core.LocalSettingsHapticTap
import com.brycewg.asrkb.ui.settings.compose.core.SettingsLayoutMetrics
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class PaymentQrDialogActions(
    val onSaveWechat: () -> Unit,
    val onSaveAlipay: () -> Unit,
    val onCopyEmail: () -> Unit,
    val onClose: () -> Unit
)

@Composable
internal fun PaymentQrDialogContent(
    uiMode: BibiUiMode,
    authorEmail: String,
    actions: PaymentQrDialogActions,
    modifier: Modifier = Modifier
) {
    ProDialogSurface(uiMode = uiMode, modifier = modifier) {
        Column(
            modifier = Modifier
                .proDialogScrollableContent()
                .padding(SettingsLayoutMetrics.ProDialogContentPadding)
        ) {
            DialogTitle(
                text = stringResource(R.string.payment_qr_title),
                uiMode = uiMode
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogMediumSpacing))
            DialogBody(
                text = stringResource(R.string.payment_qr_instruction),
                uiMode = uiMode
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogSectionSpacing))
            QrCodeRow(uiMode = uiMode, actions = actions)
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogMediumSpacing))
            EmailCopyBlock(
                uiMode = uiMode,
                authorEmail = authorEmail,
                onCopyEmail = actions.onCopyEmail
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogSmallSpacing))
            DialogTextAction(
                text = stringResource(R.string.pro_promo_btn_close),
                uiMode = uiMode,
                onClick = actions.onClose
            )
        }
    }
}

@Composable
private fun QrCodeRow(
    uiMode: BibiUiMode,
    actions: PaymentQrDialogActions
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SettingsLayoutMetrics.PaymentQrRowSpacing)
    ) {
        QrCodeCard(
            label = stringResource(R.string.payment_qr_wechat),
            drawableRes = R.drawable.qr_wechat_pay,
            uiMode = uiMode,
            onSave = actions.onSaveWechat,
            modifier = Modifier.weight(1f)
        )
        QrCodeCard(
            label = stringResource(R.string.payment_qr_alipay),
            drawableRes = R.drawable.qr_alipay,
            uiMode = uiMode,
            onSave = actions.onSaveAlipay,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QrCodeCard(
    label: String,
    drawableRes: Int,
    uiMode: BibiUiMode,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(SettingsLayoutMetrics.PaymentQrImageSize)
                .clip(RoundedCornerShape(SettingsLayoutMetrics.PaymentQrCardCorner))
                .background(Color.White)
                .padding(SettingsLayoutMetrics.PaymentQrImagePadding)
        )
        Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogSmallSpacing))
        DialogCaption(text = label, uiMode = uiMode, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogTinySpacing))
        SaveQrButton(uiMode = uiMode, onClick = onSave)
    }
}

@Composable
private fun EmailCopyBlock(
    uiMode: BibiUiMode,
    authorEmail: String,
    onCopyEmail: () -> Unit
) {
    val shape = RoundedCornerShape(SettingsLayoutMetrics.PaymentEmailCardCorner)
    val hapticTap = LocalSettingsHapticTap.current
    val copyWithHaptic = {
        hapticTap()
        onCopyEmail()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = copyWithHaptic)
            .background(
                when (uiMode) {
                    BibiUiMode.Material -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    BibiUiMode.Miuix -> MiuixTheme.colorScheme.secondaryVariant
                },
                shape
            )
            .padding(SettingsLayoutMetrics.PaymentEmailPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DialogBody(
                text = stringResource(R.string.payment_qr_email_label),
                uiMode = uiMode,
                primary = true,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(SettingsLayoutMetrics.ProDialogTinySpacing))
            DialogCaption(text = authorEmail, uiMode = uiMode, textAlign = TextAlign.Center)
            DialogCaption(
                text = stringResource(R.string.payment_qr_email_tap_copy),
                uiMode = uiMode,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SaveQrButton(uiMode: BibiUiMode, onClick: () -> Unit) {
    val hapticTap = LocalSettingsHapticTap.current
    val clickWithHaptic = {
        hapticTap()
        onClick()
    }
    when (uiMode) {
        BibiUiMode.Material -> MaterialTextButton(onClick = clickWithHaptic) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(SettingsLayoutMetrics.PaymentSaveIconSize)
            )
            MaterialText(stringResource(R.string.payment_qr_save))
        }

        BibiUiMode.Miuix -> MiuixTextButton(
            text = stringResource(R.string.payment_qr_save),
            onClick = clickWithHaptic
        )
    }
}
