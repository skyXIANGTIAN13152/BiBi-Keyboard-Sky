// Modified for Sky Wuji Edition on 2026-10-11; based on BryceWG/BiBi-Keyboard v4.4.5. See MODIFICATIONS.md.
package com.brycewg.asrkb.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable

/** Sculpted keycap drawn at native resolution; owns no input or recording behavior. */
internal class CherryKeyDrawable(
    context: Context,
    private val red: Boolean = false,
    private val legend: String = "",
    private val watermark: Boolean = false,
    private val microphone: Boolean = false
) : Drawable() {
    private val density = context.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var pressed = false
    private var enabled = true
    private var selected = false
    private var opacity = 255
    var micCaption: String = "TAP TO TALK"
        set(value) { if (field != value) { field = value; invalidateSelf() } }

    override fun isStateful() = true
    override fun onStateChange(state: IntArray): Boolean {
        val p = android.R.attr.state_pressed in state
        val e = android.R.attr.state_enabled in state
        val s = android.R.attr.state_selected in state
        if (p == pressed && e == enabled && s == selected) return false
        pressed = p; enabled = e; selected = s
        invalidateSelf()
        return true
    }

    private fun color(value: Long): Int = value.toInt()
    private fun fill(canvas: Canvas, rect: RectF, radius: Float, start: Int, end: Int) {
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, start, end, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null
    }

    override fun draw(canvas: Canvas) {
        val d = density
        if (bounds.width() < 2 || bounds.height() < 2) return
        val save = canvas.saveLayerAlpha(null, if (enabled) opacity else (opacity * .65f).toInt())
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val travel = if (pressed) 3f*d else 0f
        val radius = (if (microphone) 8f else 6f)*d
        val outer = RectF(d, d + travel, w-d, h-6f*d+travel*.35f)
        val bevel = (if (microphone) 8f else 5f)*d
        val face = RectF(outer.left+bevel, outer.top+(if(microphone) 5f else 3f)*d,
            outer.right-bevel, outer.bottom-(if (microphone) 13f else 9f)*d)
        if (face.height() < 3f*d || face.width() < 3f*d) { canvas.restoreToCount(save); return }

        // Soft cast shadow and the dark contact foot sit below all four bevels.
        paint.style = Paint.Style.FILL
        for (i in 5 downTo 1) {
            paint.color = Color.argb(if (pressed) 5 else 9, 55, 40, 31)
            canvas.drawRoundRect(RectF(outer.left-i*d*.1f,outer.top+3f*d,
                outer.right+i*d*.1f,outer.bottom+(i+1)*d*.75f), radius+d, radius+d,paint)
        }
        paint.color = if(red) color(0xff580d21) else color(0xff8a8178)
        canvas.drawRoundRect(RectF(outer.left,outer.top+2f*d,outer.right,outer.bottom+2f*d),radius,radius,paint)
        fill(canvas,outer,radius,if(red)color(0xffd66172) else color(0xfffbfaf6),if(red)color(0xff89152e) else color(0xffc9c1b7))

        val clip = canvas.save()
        path.reset();path.addRoundRect(outer,radius,radius,Path.Direction.CW);canvas.clipPath(path)
        fun side(points: FloatArray, start: Int, end: Int) {
            path.reset();path.moveTo(points[0],points[1])
            for(i in 2 until points.size step 2) path.lineTo(points[i],points[i+1])
            path.close()
            paint.shader=LinearGradient(outer.left,outer.top,outer.right,outer.bottom,start,end,Shader.TileMode.CLAMP)
            canvas.drawPath(path,paint);paint.shader=null
        }
        side(floatArrayOf(outer.left,outer.top,face.left,face.top,face.left,face.bottom,outer.left,outer.bottom),
            if(red)color(0xffe06b7c) else color(0xfff6f2ec),if(red)color(0xffb72e48) else color(0xffd8d1c7))
        side(floatArrayOf(outer.right,outer.top,face.right,face.top,face.right,face.bottom,outer.right,outer.bottom),
            if(red)color(0xffa62440) else color(0xffc5bdb3),if(red)color(0xff720d28) else color(0xffa99f94))
        side(floatArrayOf(outer.left,outer.bottom,face.left,face.bottom,face.right,face.bottom,outer.right,outer.bottom),
            if(red)color(0xffbf3d53) else color(0xffe4ded6),if(red)color(0xff7b102a) else color(0xffbcb2a6))
        canvas.restoreToCount(clip)

        paint.style=Paint.Style.STROKE;paint.strokeWidth=d*.6f
        paint.color=if(red)color(0xff780d25) else color(0xffa49a91)
        canvas.drawRoundRect(outer,radius,radius,paint)
        val fr=(if(microphone)7f else 4f)*d
        fill(canvas,face,fr,if(red)color(0xffc42c40) else color(0xfffffefa),if(red)color(0xffa61932) else color(0xffeeeae3))
        paint.style=Paint.Style.STROKE;paint.strokeWidth=d*.7f
        paint.color=if(red)color(0xfff7838d) else color(0xffffffff)
        canvas.drawRoundRect(RectF(face.left+d*.4f,face.top+d*.4f,face.right-d*.4f,face.bottom-d*.4f),fr,fr,paint)
        paint.color=if(red)color(0xfff8a1a7) else Color.WHITE
        paint.strokeWidth=d
        canvas.drawLine(face.left+6*d,face.top+1.5f*d,face.right-6*d,face.top+1.5f*d,paint)
        // A small lower rim makes the top surface distinct from the sloped wall.
        paint.color=if(red)color(0x554b031e) else color(0x338a7760)
        paint.strokeWidth=d*.8f
        canvas.drawLine(face.left+fr,face.bottom,face.right-fr,face.bottom,paint)
        paint.style=Paint.Style.FILL
        paint.typeface=Typeface.create("sans-serif",Typeface.NORMAL)
        paint.color=if(red)color(0xfffff8ee) else color(0xff92182c)
        paint.textAlign=Paint.Align.CENTER
        if (legend.isNotEmpty()) {
            paint.textSize=10f*d
            val fm=paint.fontMetrics
            canvas.drawText(legend,face.centerX(),face.centerY()-6f*d-(fm.ascent+fm.descent)/2f,paint)
        }
        if (microphone) {
            paint.textSize=minOf(9f*d,face.width()/11.5f)
            canvas.drawText(micCaption,face.centerX(),face.bottom-12f*d,paint)
        }
        if (watermark) {
            paint.textAlign=Paint.Align.RIGHT
            paint.textSize=8f*d
            paint.color=color(0xdb92182c)
            canvas.drawText("SKY.无极",face.right-6f*d,face.bottom-3f*d-paint.fontMetrics.descent,paint)
        }
        if(selected && !microphone) {
            paint.color=color(0xffb51e35)
            canvas.drawCircle(face.centerX(),face.bottom-3*d,1.1f*d,paint)
        }
        canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha: Int) { opacity=alpha;invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter=colorFilter;invalidateSelf() }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
