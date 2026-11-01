package com.bijoy.keyboard

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var p: Palette
    private lateinit var toggle: ToggleView
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var keysBox: LinearLayout
    private lateinit var chipNormal: TextView
    private lateinit var chipShift: TextView

    private class StepRow(val badge: TextView, val title: TextView, val sub: TextView, val num: String)

    private val steps = ArrayList<StepRow>()
    private var shift = false
    private var waiting = false

    // ---------- সহায়ক ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private fun dpf(v: Float) = v * resources.displayMetrics.density
    private fun white(a: Int) = Color.argb(a, 255, 255, 255)
    private val imm get() = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

    private fun enabled() = imm.enabledInputMethodList.any { it.packageName == packageName }
    private fun selected() =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.startsWith("$packageName/") == true

    private fun label(t: String, sp: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t
        textSize = sp
        setTextColor(color)
        if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        setLineSpacing(0f, 1.2f)
    }

    private fun lp(w: Int, h: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { setMargins(l, t, r, b) }

    private fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(p.card, dpf(24f), p.stroke, dp(1))
        elevation = if (p.dark) 0f else dpf(6f)
        setPadding(dp(18), dp(18), dp(18), dp(18))
    }

    private fun greeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 5..11 -> "সুপ্রভাত"
            in 12..15 -> "শুভ দুপুর"
            in 16..18 -> "শুভ বিকেল"
            in 19..21 -> "শুভ সন্ধ্যা"
            else -> "শুভ রাত্রি"
        }
    }

    // ---------- স্ক্রিন ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        p = Palette(night == Configuration.UI_MODE_NIGHT_YES)
        window.statusBarColor = p.heroA
        window.navigationBarColor = p.bg

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val side = dp(18)
        col.addView(hero(), lp(MATCH_PARENT, WRAP_CONTENT))
        col.addView(toggleCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, -dp(48), side, 0))
        col.addView(stepsCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(mapCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(testCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(tipCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(footer(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(20), side, dp(28)))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(p.bg)
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(col)
        }
        setContentView(scroll)

        // একে একে ভেসে ওঠার অ্যানিমেশন
        for (i in 0 until col.childCount) {
            val v = col.getChildAt(i)
            v.alpha = 0f
            v.translationY = dpf(28f)
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(70L * i).setDuration(450)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    // ---------- ক্রেডিট কার্ড ----------
    private var heartAnim: ObjectAnimator? = null

    private fun footer(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(26), dp(20), dp(24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR, intArrayOf(p.heroA, p.heroB)
            ).apply { cornerRadius = dpf(28f) }
            elevation = if (p.dark) 0f else dpf(8f)
        }

        // গোল ব্যাজ, নামের প্রথম অক্ষর
        val avatar = label("A", 26f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(white(40), dpf(30f), white(120), dp(2))
        }
        card.addView(avatar, lp(dp(60), dp(60)))

        card.addView(
            label("POWERED BY", 11f, white(190), true).apply {
                gravity = Gravity.CENTER
                letterSpacing = 0.25f
            },
            lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(14))
        )
        card.addView(
            label("Adnan Sami", 24f, Color.WHITE, true).apply { gravity = Gravity.CENTER },
            lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(2))
        )

        // সরু দাগ
        val line = View(this).apply { setBackgroundColor(white(60)) }
        card.addView(line, lp(dp(120), dp(1), 0, dp(16), 0, dp(14)))

        // Love Forever + স্পন্দিত হৃদয়
        val pill = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = rounded(white(38), dpf(24f), white(80), dp(1))
        }
        pill.addView(label("Love Forever", 14f, Color.WHITE, true))
        val heart = label("\u2764\uFE0F", 16f, Color.WHITE)
        pill.addView(heart, lp(WRAP_CONTENT, WRAP_CONTENT, dp(8)))
        card.addView(pill, lp(WRAP_CONTENT, WRAP_CONTENT))

        heartAnim = ObjectAnimator.ofPropertyValuesHolder(
            heart,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.3f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.3f)
        ).apply {
            duration = 650
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }

        card.addView(
            label("\u00A9 2026  \u2022  সর্বস্বত্ব সংরক্ষিত", 12f, white(200)).apply { gravity = Gravity.CENTER },
            lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(16))
        )
        return card
    }

    override fun onDestroy() {
        heartAnim?.cancel()
        super.onDestroy()
    }

    private fun hero(): View {
        val r = dpf(36f)
        val box = FrameLayout(this)
        box.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(p.heroA, p.heroB)
        ).apply { cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r) }
        box.addView(BlobView(this, r), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(26), dp(24), dp(78))
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val badge = label("অ", 26f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(white(45), dpf(16f), white(90), dp(1))
        }
        top.addView(badge, lp(dp(52), dp(52)))
        val pill = label("স্বাগতম  •  ${greeting()}", 13f, Color.WHITE, true).apply {
            setPadding(dp(14), dp(7), dp(14), dp(7))
            background = rounded(white(45), dpf(30f))
        }
        top.addView(pill, lp(WRAP_CONTENT, WRAP_CONTENT, dp(12)))
        col.addView(top)

        col.addView(label("বিজয় ফিজিক্যাল\nকিবোর্ড", 32f, Color.WHITE, true), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(22)))
        col.addView(
            label(
                "OTG বা যেকোনো কিবোর্ডে ইংরেজি কী চাপুন, যেকোনো অ্যাপে নিখুঁত বাংলা পান। কম্পিউটারের বিজয় বাহান্ন (৫২) লেআউটের মতোই।",
                14.5f, white(225)
            ), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(10))
        )

        val feats = LinearLayout(this)
        for ((i, f) in listOf("বিজয় ৫২", "যেকোনো অ্যাপে", "অফলাইন").withIndex()) {
            val chip = label(f, 12f, Color.WHITE, true).apply {
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = rounded(white(38), dpf(20f), white(70), dp(1))
            }
            feats.addView(chip, lp(WRAP_CONTENT, WRAP_CONTENT, if (i == 0) 0 else dp(8)))
        }
        col.addView(feats, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(18)))

        box.addView(col, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return box
    }

    private fun toggleCard(): View {
        val c = card()
        c.gravity = Gravity.CENTER_HORIZONTAL
        toggle = ToggleView(this, p).apply { onTap = { onToggle() } }
        c.addView(toggle, lp(dp(250), dp(100)))
        statusTitle = label("", 20f, p.text, true).apply { gravity = Gravity.CENTER }
        statusSub = label("", 14f, p.sub).apply { gravity = Gravity.CENTER }
        c.addView(statusTitle, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(2)))
        c.addView(statusSub, lp(MATCH_PARENT, WRAP_CONTENT, dp(6), dp(6), dp(6), 0))
        return c
    }

    private fun stepRow(num: String, title: String, sub: String): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val badge = label(num, 15f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        row.addView(badge, lp(dp(38), dp(38)))
        val txt = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val t = label(title, 16f, p.text, true)
        val s = label(sub, 13f, p.sub)
        txt.addView(t)
        txt.addView(s, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(2)))
        row.addView(txt, lp(0, WRAP_CONTENT, dp(14)).apply { weight = 1f })
        steps.add(StepRow(badge, t, s, num))
        return row
    }

    private fun stepsCard(): View {
        val c = card()
        c.addView(label("কীভাবে ব্যবহার করবেন", 18f, p.text, true))
        c.addView(stepRow("১", "কিবোর্ড চালু করুন", "উপরের সুইচ চেপে সেটিংসে “বিজয় ফিজিক্যাল কিবোর্ড” অন করুন"), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(16)))
        c.addView(stepRow("২", "কিবোর্ড বেছে নিন", "তালিকা থেকে বিজয় কিবোর্ডটি নির্বাচন করুন"), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(16)))
        c.addView(stepRow("৩", "OTG কিবোর্ড লাগিয়ে লিখুন", "যেকোনো অ্যাপে ইংরেজি কী চাপলেই বাংলা আসবে"), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(16)))
        return c
    }

    // ---------- কী-ম্যাপ ----------
    private fun chipStyle(chip: TextView, active: Boolean) {
        chip.setTextColor(if (active) Color.WHITE else p.sub)
        chip.background = rounded(if (active) p.primary else p.key, dpf(20f))
    }

    private fun mapCard(): View {
        val c = card()
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(label("বিজয় কী-ম্যাপ", 18f, p.text, true), lp(0, WRAP_CONTENT).apply { weight = 1f })
        chipNormal = label("সাধারণ", 13f, p.sub, true).apply { setPadding(dp(14), dp(7), dp(14), dp(7)) }
        chipShift = label("Shift", 13f, p.sub, true).apply { setPadding(dp(14), dp(7), dp(14), dp(7)) }
        chipNormal.setOnClickListener { shift = false; refreshKeys() }
        chipShift.setOnClickListener { shift = true; refreshKeys() }
        head.addView(chipNormal)
        head.addView(chipShift, lp(WRAP_CONTENT, WRAP_CONTENT, dp(6)))
        c.addView(head)
        c.addView(label("কীক্যাপের ইংরেজি অক্ষরের নিচে যে বাংলা লেখা, সেটিই আসবে।", 13f, p.sub), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(6), 0, dp(12)))

        keysBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        c.addView(keysBox)
        refreshKeys()
        return c
    }

    private fun refreshKeys() {
        chipStyle(chipNormal, !shift)
        chipStyle(chipShift, shift)
        keysBox.removeAllViews()
        val shiftSym = mapOf(
            '`' to '~', '1' to '!', '2' to '@', '3' to '#', '4' to '$', '5' to '%',
            '6' to '^', '7' to '&', '8' to '*', '9' to '(', '0' to ')', ';' to ':'
        )
        val rows = listOf("`1234567890", "qwertyuiop", "asdfghjkl;", "zxcvbnm")
        rows.forEachIndexed { i, chars ->
            val row = LinearLayout(this)
            if (i == 3) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1.5f))
            for (ch in chars) {
                val typed = if (!shift) ch else if (ch.isLetter()) ch.uppercaseChar() else (shiftSym[ch] ?: ch)
                row.addView(keyCell(ch, typed), lp(0, WRAP_CONTENT, dp(2), 0, dp(2), 0).apply { weight = 1f })
            }
            if (i == 3) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1.5f))
            keysBox.addView(row, lp(MATCH_PARENT, WRAP_CONTENT, 0, if (i == 0) 0 else dp(5)))
        }
    }

    private fun keyCell(cap: Char, typed: Char): View {
        val bn = BijoyEngine.MAP[typed]
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(5), 0, dp(6))
            background = if (bn != null) rounded(p.key, dpf(10f))
            else rounded(Color.TRANSPARENT, dpf(10f), p.stroke, dp(1))
        }
        val capText = label(if (shift && !cap.isLetter()) typed.toString() else cap.uppercaseChar().toString(), 10f, p.sub)
            .apply { gravity = Gravity.CENTER }
        val marks = "ািীুূৃেৈোৌৗ্ঁ"
        val shown = when {
            bn == null -> "·"
            bn.first() in marks -> "◌$bn"
            else -> bn
        }
        val bnText = label(shown, 16f, if (bn != null) p.text else p.sub, true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
        }
        cell.addView(capText, lp(MATCH_PARENT, WRAP_CONTENT))
        cell.addView(bnText, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(1)))
        return cell
    }

    // ---------- টেস্ট বক্স ও টিপস ----------
    private fun testCard(): View {
        val c = card()
        c.addView(label("এখানে লিখে পরীক্ষা করুন", 18f, p.text, true))
        c.addView(label("কিবোর্ড চালু করে এই ঘরে ইংরেজি কী চাপুন, বাংলা এলে সব ঠিক আছে।", 13f, p.sub), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(6), 0, dp(12)))
        val et = EditText(this).apply {
            hint = "এখানে টাইপ করে দেখুন…"
            setHintTextColor(p.sub)
            setTextColor(p.text)
            textSize = 20f
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(p.key, dpf(16f), p.stroke, dp(1))
        }
        c.addView(et, lp(MATCH_PARENT, WRAP_CONTENT))
        return c
    }

    private fun tipCard(): View {
        val c = card()
        c.addView(label("বাংলা ⇄ English", 18f, p.text, true))
        c.addView(label("দ্রুত ভাষা বদলাতে এই তিনটি কী একসাথে চাপুন।", 13f, p.sub), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(6), 0, dp(14)))
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val parts = listOf("Ctrl", "+", "Alt", "+", "B")
        for ((i, s) in parts.withIndex()) {
            val isKey = s != "+"
            val v = label(s, if (isKey) 16f else 18f, if (isKey) p.text else p.sub, true).apply {
                gravity = Gravity.CENTER
                if (isKey) {
                    setPadding(dp(16), dp(10), dp(16), dp(12))
                    background = rounded(p.key, dpf(12f), p.stroke, dp(1))
                }
            }
            row.addView(v, lp(WRAP_CONTENT, WRAP_CONTENT, if (i == 0) 0 else dp(8)))
        }
        c.addView(row)
        return c
    }

    // ---------- আচরণ ----------
    private fun onToggle() {
        when {
            !enabled() -> {
                waiting = true
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
            !selected() -> imm.showInputMethodPicker()
            else -> {
                Toast.makeText(this, "বন্ধ করতে তালিকা থেকে অন্য কিবোর্ড বেছে নিন", Toast.LENGTH_SHORT).show()
                imm.showInputMethodPicker()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (waiting && enabled() && !selected()) {
            waiting = false
            toggle.postDelayed({ imm.showInputMethodPicker() }, 400)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refresh()
    }

    private fun refresh() {
        val en = enabled()
        val sel = selected()
        toggle.setOn(sel)
        when {
            !en -> {
                statusTitle.text = "কিবোর্ড এখনো বন্ধ"
                statusSub.text = "সুইচটি চাপুন, সেটিংসে গিয়ে “বিজয় ফিজিক্যাল কিবোর্ড” চালু করুন"
            }
            !sel -> {
                statusTitle.text = "প্রায় হয়ে গেছে"
                statusSub.text = "সুইচ চেপে বিজয় কিবোর্ডটি বেছে নিন"
            }
            else -> {
                statusTitle.text = "বিজয় কিবোর্ড চালু ✓"
                statusSub.text = "এখন যেকোনো অ্যাপে ইংরেজি কী চাপুন, বাংলা আসবে"
            }
        }
        statusTitle.setTextColor(if (sel) p.ok else p.text)

        val done = booleanArrayOf(en, sel, false)
        val current = done.indexOfFirst { !it }
        steps.forEachIndexed { i, s ->
            when {
                done[i] -> {
                    s.badge.text = "✓"
                    s.badge.background = oval(p.ok)
                    s.badge.setTextColor(Color.WHITE)
                }
                i == current -> {
                    s.badge.text = s.num
                    s.badge.background = oval(p.primary)
                    s.badge.setTextColor(Color.WHITE)
                }
                else -> {
                    s.badge.text = s.num
                    s.badge.background = oval(p.key)
                    s.badge.setTextColor(p.sub)
                }
            }
        }
    }
}
