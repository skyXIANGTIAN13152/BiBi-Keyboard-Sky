package com.brycewg.asrkb.ime

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.brycewg.asrkb.R
import com.brycewg.asrkb.store.Prefs

internal class NumpadPanelController(
    private val prefs: Prefs,
    private val views: ImeViewRefs,
    private val inputHelper: InputConnectionHelper,
    private val actionHandler: KeyboardActionHandler,
    private val backspaceGestureHandler: BackspaceGestureHandler,
    private val performKeyHaptic: (View?) -> Unit,
    private val inputConnectionProvider: () -> android.view.inputmethod.InputConnection?,
    private val editorInfoProvider: () -> android.view.inputmethod.EditorInfo?,
    private val onRequestShowAiEditPanel: () -> Unit
) {
    var isVisible: Boolean = views.layoutNumpadPanel?.visibility == View.VISIBLE
        private set

    private var returnToAiPanel: Boolean = false

    fun bindListeners() {
        views.btnNumpadBack?.setOnClickListener { v ->
            performKeyHaptic(v)
            val shouldReturnToAiPanel = returnToAiPanel
            hide()
            if (shouldReturnToAiPanel) {
                onRequestShowAiEditPanel()
            } else {
                views.layoutMainKeyboard?.visibility = View.VISIBLE
            }
        }

        views.btnNumpadEnter?.setOnClickListener { v ->
            performKeyHaptic(v)
            inputHelper.sendEnter(inputConnectionProvider(), editorInfoProvider())
        }

        views.btnNumpadBackspace?.setOnClickListener { v ->
            performKeyHaptic(v)
            actionHandler.saveUndoSnapshot(inputConnectionProvider())
            inputHelper.sendBackspace(inputConnectionProvider())
        }
        views.btnNumpadBackspace?.setOnTouchListener { v, ev ->
            backspaceGestureHandler.handleTouchEvent(v, ev, inputConnectionProvider())
        }

        bindNumpadKeys()

        views.btnNumpadPunctToggle?.setOnClickListener { v ->
            performKeyHaptic(v)
            prefs.numpadCnPunctEnabled = !prefs.numpadCnPunctEnabled
            applyPunctMode()
        }

        applyPunctMode()
    }

    fun show(returnToAiPanel: Boolean) {
        if (isVisible) return
        this.returnToAiPanel = returnToAiPanel
        val mainHeight = views.rootView.findViewById<View>(R.id.keyboardLayoutCanvas)?.height?.takeIf { it > 0 }
            ?: views.layoutMainKeyboard?.height?.takeIf { it > 0 }
            ?: (
                (views.rootView.height - views.rootView.paddingTop - views.rootView.paddingBottom).takeIf {
                    it >
                        0
                }
                )
        views.layoutAiEditPanel?.visibility = View.GONE
        views.layoutMainKeyboard?.visibility = View.GONE
        val panel = views.layoutNumpadPanel
        if (panel != null) {
            if (mainHeight != null && mainHeight > 0) {
                val lp = panel.layoutParams
                lp.height = mainHeight
                panel.layoutParams = lp
            }
            panel.visibility = View.VISIBLE
        }
        views.groupMicStatus?.visibility = View.GONE
        isVisible = true
        applyPunctMode()
    }

    fun hide() {
        val shouldReturnToAiPanel = returnToAiPanel
        if (!isVisible) {
            if (!shouldReturnToAiPanel) {
                views.layoutMainKeyboard?.visibility = View.VISIBLE
                views.groupMicStatus?.visibility = View.VISIBLE
            }
            returnToAiPanel = false
            return
        }
        val panel = views.layoutNumpadPanel
        if (panel != null) {
            panel.visibility = View.GONE
            val lp = panel.layoutParams
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            panel.layoutParams = lp
        }
        if (!shouldReturnToAiPanel) {
            views.groupMicStatus?.visibility = View.VISIBLE
            views.layoutMainKeyboard?.visibility = View.VISIBLE
        }
        isVisible = false
        returnToAiPanel = false
    }

    private fun bindNumpadKeys() {
        val root = views.layoutNumpadPanel ?: return
        fun bindView(v: View) {
            val tag = v.tag as? String
            if (tag == "key40" && v is TextView) {
                v.setOnClickListener { btn ->
                    performKeyHaptic(btn)
                    val ic = inputConnectionProvider() ?: return@setOnClickListener
                    val text = when (v.id) {
                        R.id.np_key_space -> " "
                        else -> v.text?.toString() ?: ""
                    }
                    if (text.isNotEmpty()) {
                        actionHandler.commitText(ic, text)
                    }
                }
            }
            if (v is android.view.ViewGroup) {
                for (i in 0 until v.childCount) {
                    bindView(v.getChildAt(i))
                }
            }
        }
        bindView(root)
    }

    private fun applyPunctMode() {
        val root = views.layoutNumpadPanel ?: return
        val cn = prefs.numpadCnPunctEnabled
        views.btnNumpadPunctToggle?.setImageResource(
            if (cn) R.drawable.translate_fill else R.drawable.translate
        )

        val row1 = root.findViewById<View>(R.id.rowPunct1) as? android.view.ViewGroup
        val row2 = root.findViewById<View>(R.id.rowPunct2) as? android.view.ViewGroup
        val cn1 = arrayOf("，", "。", "、", "！", "？", "：", "；", "“", "”", "@")
        val en1 = arrayOf(",", ".", ",", "!", "?", ":", ";", "\"", "\"", "@")
        if (row1 != null) {
            val arr = if (cn) cn1 else en1
            val count = minOf(row1.childCount, arr.size)
            for (i in 0 until count) {
                val tv = row1.getChildAt(i)
                if (tv is TextView) tv.text = arr[i]
            }
        }

        val cn2 = arrayOf("（", "）", "[", "]", "{", "}", "/", "`")
        val en2 = arrayOf("(", ")", "[", "]", "{", "}", "/", "`")
        if (row2 != null) {
            val arr = if (cn) cn2 else en2
            val count = minOf(row2.childCount, arr.size)
            for (i in 0 until count) {
                val v = row2.getChildAt(i)
                if (v is TextView) v.text = arr[i]
            }
        }
    }
}
