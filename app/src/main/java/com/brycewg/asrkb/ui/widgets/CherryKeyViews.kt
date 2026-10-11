// Modified for Sky Wuji Edition on 2026-10-11; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.widget.Button
import android.widget.ImageButton
import com.brycewg.asrkb.R

/** Move only the legend; the drawable keeps the contact shadow fixed. */
internal class CherryImageButton(context: Context) : ImageButton(context) {
    override fun onDraw(canvas: Canvas) {
        val d=resources.displayMetrics.density
        val saved=canvas.save()
        val travel=if(isPressed) 3f*d else 0f
        val isMic=id==R.id.btnMic || id==R.id.btnAiPanelMic
        val isBack=id==R.id.btnAiEdit
        canvas.translate(0f,(if(isMic) -12f else if(isBack) 3f else -6f)*d+travel)
        if(isBack) canvas.scale(.68f,.68f,width/2f,height/2f)
        else if(!isMic) canvas.scale(.84f,.84f,width/2f,height/2f)
        super.onDraw(canvas)
        canvas.restoreToCount(saved)
    }
}

internal class CherryTextButton(context: Context) : Button(context) {
    override fun onDraw(canvas: Canvas) {
        val saved=canvas.save()
        canvas.translate(0f,((if(id==R.id.btnExtCenter2) -9f else -6f)+(if(isPressed)3f else 0f))*resources.displayMetrics.density)
        super.onDraw(canvas)
        canvas.restoreToCount(saved)
    }
}
