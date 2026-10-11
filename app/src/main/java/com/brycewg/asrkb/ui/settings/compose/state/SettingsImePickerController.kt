/**
 * 设置页外部入口输入法选择器流程协调器。
 *
 * 归属模块：ui/settings/compose/state
 */
package com.brycewg.asrkb.ui.settings.compose.state

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.brycewg.asrkb.ui.BaseActivity

internal class SettingsImePickerController(
    private val activity: BaseActivity,
    private val handler: Handler,
    private val autoShowExtra: String,
    private val showExtra: String
) {
    private var autoCloseAfterImePicker = false
    private var imePickerShown = false
    private var imePickerLostFocusOnce = false
    private var autoShownImePicker = false
    private var qsTileImePickerRequested = false

    fun consumeShowImePickerExtraIfPresent(intent: Intent?) {
        if (intent?.getBooleanExtra(showExtra, false) != true) return
        intent.removeExtra(showExtra)
        qsTileImePickerRequested = true
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        handleExternalImeSwitchMode(hasFocus)
        handleAutoShowImePicker(hasFocus)
        handleShowImePickerFromTile(hasFocus)
    }

    fun handleShowImePickerFromTile(hasFocus: Boolean) {
        if (!hasFocus) return
        if (!qsTileImePickerRequested) return

        qsTileImePickerRequested = false

        handler.post {
            val imm = activity.getSystemService(InputMethodManager::class.java)
            autoCloseAfterImePicker = true
            imePickerShown = true
            imePickerLostFocusOnce = false
            imm?.showInputMethodPicker()
        }
    }

    private fun handleExternalImeSwitchMode(hasFocus: Boolean) {
        if (!autoCloseAfterImePicker || !imePickerShown) {
            return
        }

        if (!hasFocus) {
            imePickerLostFocusOnce = true
            Log.d(TAG, "IME picker shown, activity lost focus")
        } else if (imePickerLostFocusOnce) {
            Log.d(TAG, "IME picker closed, activity regained focus, finishing")
            handler.postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.finish()
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            activity.overrideActivityTransition(
                                Activity.OVERRIDE_TRANSITION_CLOSE,
                                0,
                                0
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            activity.overridePendingTransition(0, 0)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to override pending transition", e)
                    }
                }
            }, 250L)

            imePickerShown = false
            imePickerLostFocusOnce = false
        }
    }

    private fun handleAutoShowImePicker(hasFocus: Boolean) {
        if (!hasFocus) return
        if (autoShownImePicker) return
        if (activity.intent?.getBooleanExtra(autoShowExtra, false) != true) return

        autoShownImePicker = true
        autoCloseAfterImePicker = true

        Log.d(TAG, "Auto-showing IME picker from intent extra")

        handler.post {
            try {
                val imm = activity.getSystemService(InputMethodManager::class.java)
                imm?.showInputMethodPicker()
                imePickerShown = true
                imePickerLostFocusOnce = false
                Log.d(TAG, "IME picker shown successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show IME picker", e)
            }
        }
    }

    private companion object {
        private const val TAG = "SettingsImePickerController"
    }
}
