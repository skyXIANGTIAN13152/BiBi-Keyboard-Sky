package com.brycewg.asrkb.ui.floating

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.brycewg.asrkb.R

internal class OverlayPermissionGate(
    private val context: Context,
    private val notifier: UserNotifier,
    private val tag: String
) {
    fun hasPermission(): Boolean = Settings.canDrawOverlays(context)

    fun showMissingPermissionToast() {
        try {
            notifier.showToast(context.getString(R.string.toast_need_overlay_perm))
        } catch (e: Throwable) {
            Log.w(tag, "Failed to show overlay permission toast", e)
        }
    }

    fun openSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            Log.e(tag, "Failed to open overlay permission settings", e)
        }
    }

    fun requestPermission() {
        showMissingPermissionToast()
        openSettings()
    }
}
