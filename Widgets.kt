package com.bijoy.keyboard

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import android.view.animation.OvershootInterpolator

/** হিরোর পেছনের নরম গোল আকৃতিগুলো */
class BlobView(ctx: Context, private val r: Float) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        clip.reset()
        clip.addRoundRect(RectF(0f, 0f, w, h), floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r), Path.Direction.CW)
        c.clipPath(clip)
        paint.color = Color.argb(34, 255, 255, 255)
        c.drawCircle(w * 0.92f, h * 0.12f, h * 0.5f, paint)
        paint.color = Color.argb(24, 255, 255, 255)
        c.drawCircle(w * 0.02f, h * 0.98f, h * 0.42f, paint)
        paint.color = Color.argb(16, 255, 255, 255)
        c.drawCircle(w * 0.55f, h * 0.9f, h * 0.2f, paint)
    }
}

/** অ্যানিমেটেড বড় ON/OFF সুইচ */
class ToggleView(ctx: Context, p: Palette) : View(ctx) {
    var onTap: (() -> Unit)? = null
    var on = false
        private set

    private var t = 0f
    private var anim: ValueAnimator? = null
    private val d = resources.displayMetrics.density
    private val offC = p.offTrack
    private val onC = p.primary
    private val subC = p.sub
    private val eval = ArgbEvaluator()
    private val rect = RectF()
    private val arc = RectF()

    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3f * d
    }
    private val lab = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        textSize = 17f * d
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
        contentDescription = "কিবোর্ড সুইচ"
        setOnClickListener { onTap?.invoke() }
    }

    fun setOn(v: Boolean) {
        if (v == on) return
        on = v
        anim?.cancel()
        val target = if (v) 1f else 0f
        if (!isLaidOut) {
            t = target
            invalidate()
            return
        }
        anim = ValueAnimator.ofFloat(t, target).apply {
            duration = 420
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { t = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(c: Canvas) {
        val inset = 10f * d
        rect.set(inset, inset, width - inset, height - inset)
        val h = rect.height()
        val rad = h / 2f
        val tc = t.coerceIn(0f, 1f)

        // ট্র্যাক + জ্বলজ্বলে আভা
        track.color = eval.evaluate(tc, offC, onC) as Int
        track.setShadowLayer(
            14f * d * tc, 0f, 4f * d * tc,
            Color.argb((110 * tc).toInt(), Color.red(onC), Color.green(onC), Color.blue(onC))
        )
        c.drawRoundRect(rect, rad, rad, track)

        // লেখা
        val cy = rect.centerY() - (lab.ascent() + lab.descent()) / 2f
        lab.color = Color.argb((255 * tc).toInt(), 255, 255, 255)
        c.drawText("চালু", rect.left + rect.width() * 0.28f, cy, lab)
        lab.color = (subC and 0x00FFFFFF) or ((255 * (1f - tc)).toInt() shl 24)
        c.drawText("বন্ধ", rect.left + rect.width() * 0.72f, cy, lab)

        // নব
        val kr = rad - 7f * d
        val cx = rect.left + rad + (rect.width() - 2f * rad) * t
        val ky = rect.centerY()
        knob.setShadowLayer(8f * d, 0f, 2f * d, Color.argb(70, 0, 0, 0))
        c.drawCircle(cx, ky, kr, knob)

        // পাওয়ার আইকন
        icon.color = eval.evaluate(tc, subC, onC) as Int
        val ir = kr * 0.42f
        arc.set(cx - ir, ky - ir, cx + ir, ky + ir)
        c.drawArc(arc, -60f, 300f, false, icon)
        c.drawLine(cx, ky - ir * 1.15f, cx, ky - ir * 0.1f, icon)
    }
}

/** কাঁচের কার্ডের পেছনের রঙিন নরম আলো: গ্লাস ইফেক্ট এর ওপর দিয়েই ফুটে ওঠে */
class GlassBackdrop(ctx: Context, private val dark: Boolean, private val r: Float) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()

    private fun blob(c: Canvas, x: Float, y: Float, rad: Float, color: Int, a: Int) {
        paint.shader = RadialGradient(
            x, y, rad,
            Color.argb(a, Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
            Shader.TileMode.CLAMP
        )
        c.drawCircle(x, y, rad, paint)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        clip.reset()
        clip.addRoundRect(RectF(0f, 0f, w, h), r, r, Path.Direction.CW)
        c.clipPath(clip)

        paint.shader = LinearGradient(
            0f, 0f, w, h,
            Color.parseColor(if (dark) "#0F1A40" else "#DCE7FF"),
            Color.parseColor(if (dark) "#2A1A5C" else "#EEE3FF"),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, paint)

        val a = if (dark) 200 else 190
        blob(c, w * 0.12f, h * 0.18f, w * 0.55f, Color.parseColor("#4F7BFF"), a)
        blob(c, w * 0.92f, h * 0.42f, w * 0.50f, Color.parseColor("#9B6BFF"), a)
        blob(c, w * 0.30f, h * 0.95f, w * 0.55f, Color.parseColor("#38C9E8"), if (dark) 150 else 140)
        blob(c, w * 0.85f, h * 0.05f, w * 0.30f, Color.parseColor("#FF8FC8"), if (dark) 120 else 120)
        paint.shader = null
    }
}
