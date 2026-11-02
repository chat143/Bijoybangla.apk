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

/** পুরো অ্যাপের পেছনের হালকা সাদা-ধূসর পটভূমি, নরম আলোর দাগ সহ (কাঁচের কার্ড এর ওপরেই ফুটে ওঠে) */
class PageBackdrop(ctx: Context) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun blob(c: Canvas, x: Float, y: Float, rad: Float, gray: Int, a: Int) {
        paint.shader = RadialGradient(
            x, y, rad,
            Color.argb(a, gray, gray, gray),
            Color.argb(0, gray, gray, gray),
            Shader.TileMode.CLAMP
        )
        c.drawCircle(x, y, rad, paint)
        paint.shader = null
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.shader = LinearGradient(
            0f, 0f, w, h,
            Color.parseColor("#FBFBFC"), Color.parseColor("#E9ECF0"),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        blob(c, w * 0.95f, h * 0.06f, w * 0.75f, 150, 90)
        blob(c, w * 0.00f, h * 0.38f, w * 0.65f, 120, 60)
        blob(c, w * 0.90f, h * 0.68f, w * 0.70f, 140, 70)
        blob(c, w * 0.10f, h * 0.95f, w * 0.80f, 110, 55)
    }
}

/** কাঁচের কার্ডের পেছনের কালো-সাদা নরম আভা: ক্রেডিট কার্ডে ব্যবহৃত */
class GlassBackdrop(ctx: Context, private val r: Float) : View(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * resources.displayMetrics.density
        color = Color.argb(70, 255, 255, 255)
    }
    private val clip = Path()

    private fun blob(c: Canvas, x: Float, y: Float, rad: Float, gray: Int, a: Int) {
        paint.shader = RadialGradient(
            x, y, rad,
            Color.argb(a, gray, gray, gray),
            Color.argb(0, gray, gray, gray),
            Shader.TileMode.CLAMP
        )
        c.drawCircle(x, y, rad, paint)
        paint.shader = null
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        clip.reset()
        clip.addRoundRect(RectF(0f, 0f, w, h), r, r, Path.Direction.CW)
        c.clipPath(clip)

        paint.shader = LinearGradient(
            0f, 0f, w, h,
            Color.parseColor("#FAFBFC"), Color.parseColor("#E6E9EE"),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        blob(c, w * 0.10f, h * 0.15f, w * 0.60f, 255, 230)
        blob(c, w * 0.95f, h * 0.40f, w * 0.55f, 150, 90)
        blob(c, w * 0.25f, h * 0.95f, w * 0.60f, 130, 80)

        // কাঁচের ওপর বাঁকা আভার রেখা
        c.drawCircle(w * 0.15f, h * 0.55f, w * 0.55f, line)
        c.drawCircle(w * 1.02f, h * 0.95f, w * 0.60f, line)
    }
}

/** কালো-সাদা কীবোর্ড লোগো: কালো গোল-চৌকো ঘরে সাদা কী */
class KeyboardLogo(ctx: Context) : View(ctx) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B0B0D") }
    private val key = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val rect = RectF()

    override fun onDraw(c: Canvas) {
        val s = width.toFloat()
        rect.set(0f, 0f, s, s)
        c.drawRoundRect(rect, s * 0.28f, s * 0.28f, bg)

        val kr = s * 0.045f
        val gap = s * 0.055f
        val kh = s * 0.145f
        val left = s * 0.17f
        val inner = s - 2 * left

        // সারি ১: ৪টি কী
        var y = s * 0.26f
        val w4 = (inner - 3 * gap) / 4f
        for (i in 0 until 4) {
            val x = left + i * (w4 + gap)
            rect.set(x, y, x + w4, y + kh)
            c.drawRoundRect(rect, kr, kr, key)
        }
        // সারি ২: ৪টি কী
        y += kh + gap
        for (i in 0 until 4) {
            val x = left + i * (w4 + gap)
            rect.set(x, y, x + w4, y + kh)
            c.drawRoundRect(rect, kr, kr, key)
        }
        // সারি ৩: স্পেসবার
        y += kh + gap
        rect.set(left + w4 + gap, y, s - left - w4 - gap, y + kh)
        c.drawRoundRect(rect, kr, kr, key)
    }
}

/** অ্যানিমেটেড বড় ON/OFF সুইচ (কালো-সাদা) */
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

        // ট্র্যাক + নরম ছায়া
        track.color = eval.evaluate(tc, offC, onC) as Int
        track.setShadowLayer(
            12f * d * tc, 0f, 4f * d * tc,
            Color.argb((70 * tc).toInt(), 0, 0, 0)
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
