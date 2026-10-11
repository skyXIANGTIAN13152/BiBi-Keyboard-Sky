/**
 * 自定义键盘布局运行时 View 标记约定。
 *
 * 归属模块：ime/layout
 */
package com.brycewg.asrkb.ime.layout

import android.view.View
import com.brycewg.asrkb.R

internal object KeyboardLayoutViewTags {
    fun markBlockView(view: View, panel: KeyboardLayoutPanel, def: BlockDef, dynamic: Boolean) {
        view.setTag(R.id.keyboardLayoutBlockPanelTag, panel.id)
        view.setTag(R.id.keyboardLayoutBlockDefTag, def.id)
        view.setTag(R.id.keyboardLayoutBlockDynamicTag, dynamic)
    }

    fun panelOf(view: View): KeyboardLayoutPanel? = KeyboardLayoutPanel.values().firstOrNull {
        view.getTag(R.id.keyboardLayoutBlockPanelTag) == it.id
    }

    fun defIdOf(view: View): String? = view.getTag(R.id.keyboardLayoutBlockDefTag) as? String

    fun isDynamicBlockView(view: View): Boolean = view.getTag(R.id.keyboardLayoutBlockDynamicTag) == true
}
