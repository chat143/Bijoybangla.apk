package com.bijoy.keyboard

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
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
        background = glass(dpf(24f), true)
        elevation = dpf(5f)
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
        p = Palette()

        // ফোনে ডার্ক মোড চালু থাকলেও অ্যাপ সবসময় সাদা থিমে থাকবে
        window.statusBarColor = Color.parseColor("#FBFBFC")
        window.navigationBarColor = Color.parseColor("#E9ECF0")
        var flags = window.decorView.systemUiVisibility
        if (Build.VERSION.SDK_INT >= 23) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 26) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.decorView.systemUiVisibility = flags
        if (Build.VERSION.SDK_INT >= 29) window.decorView.isForceDarkAllowed = false

        // পেইড ভার্সনে লাইসেন্স সক্রিয় না থাকলে — লগইন পেজের মতো আলাদা একটা পেজ, মূল অ্যাপের আগে
        if (BuildConfig.IS_PAID && !License.isLicensed(this)) {
            showLicenseGate()
            return
        }
        buildMainScreen()
    }

    /** লাইসেন্স না থাকলে প্রথমেই এই পেজটা দেখানো হয় — আইডি, ইনপুট বক্স, সাবমিট, নিচে যোগাযোগ */
    private fun showLicenseGate() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val side = dp(16)
        col.addView(header(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(40), side, 0))
        col.addView(licenseCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(contactRow(), lp(WRAP_CONTENT, WRAP_CONTENT, 0, dp(22), 0, dp(32)))

        val scroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(col)
        }
        val root = FrameLayout(this)
        root.addView(PageBackdrop(this), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(scroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)

        for (i in 0 until col.childCount) {
            val v = col.getChildAt(i)
            v.alpha = 0f
            v.translationY = dpf(28f)
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(70L * i).setDuration(450)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun buildMainScreen() {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val side = dp(16)
        col.addView(header(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(14), side, 0))
        if (!BuildConfig.IS_PAID) {
            col.addView(trialCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        }
        col.addView(toggleCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(stepsCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(mapCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(testCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(tipCard(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(16), side, 0))
        col.addView(footer(), lp(MATCH_PARENT, WRAP_CONTENT, side, dp(20), side, dp(28)))

        val scroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(col)
        }
        val root = FrameLayout(this)
        root.addView(PageBackdrop(this), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        root.addView(scroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)

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

    // ---------- ক্রেডিট (গ্লাস কার্ড) ----------
    private var heartAnim: ObjectAnimator? = null

    private val circleOutline = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setOval(0, 0, view.width, view.height)
        }
    }

    /** স্বচ্ছ কাঁচের মতো পটভূমি: আধা-সাদা গ্রেডিয়েন্ট + উজ্জ্বল সাদা কিনারা */
    private fun glass(radius: Float, strong: Boolean = false): GradientDrawable {
        val top = Color.argb(if (strong) 215 else 165, 255, 255, 255)
        val bot = Color.argb(if (strong) 130 else 90, 255, 255, 255)
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(top, bot)).apply {
            cornerRadius = radius
            setStroke(dp(1), Color.argb(240, 255, 255, 255))
        }
    }

    private fun hairline(): View = View(this).apply {
        setBackgroundColor(Color.argb(70, 0, 0, 0))
    }

    private fun decodeSampled(resId: Int, target: Int): android.graphics.Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, resId, o)
        var s = 1
        while (o.outWidth / (s * 2) >= target && o.outHeight / (s * 2) >= target) s *= 2
        return BitmapFactory.decodeResource(resources, resId, BitmapFactory.Options().apply { inSampleSize = s })
    }

    /** গোল ছবি + কাঁচের রিং + </> ব্যাজ। ছবি না থাকলে "A" দেখাবে */
    private fun avatar(): View {
        val size = dp(112)
        val inset = dp(9)
        val frame = FrameLayout(this)

        val ring = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(white(120))
                setStroke(dp(3), white(235))
            }
        }
        frame.addView(ring, FrameLayout.LayoutParams(size, size))

        val id = resources.getIdentifier("adnan", "drawable", packageName)
        val bmp = if (id != 0) decodeSampled(id, dp(220)) else null
        val photo: View = if (bmp != null) {
            ImageView(this).apply { setImageBitmap(bmp); scaleType = ImageView.ScaleType.CENTER_CROP }
        } else {
            label("A", 36f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = oval(p.primary)
            }
        }
        photo.outlineProvider = circleOutline
        photo.clipToOutline = true
        frame.addView(photo, FrameLayout.LayoutParams(size - 2 * inset, size - 2 * inset, Gravity.CENTER))

        val badge = label("</>", 12f, p.text, true).apply {
            gravity = Gravity.CENTER
            background = oval(Color.WHITE)
            elevation = dpf(5f)
        }
        frame.addView(badge, FrameLayout.LayoutParams(dp(34), dp(34), Gravity.BOTTOM or Gravity.END).apply {
            setMargins(0, 0, dp(2), dp(4))
        })
        return frame
    }

    // ---------- যোগাযোগ (Facebook / WhatsApp / ফোন) ----------
    private val facebookUrl = "https://www.facebook.com/dev.adnan.sami143"
    private val phoneNumber = "01889157642"
    private val whatsappNumber = "8801889157642" // দেশের কোডসহ, ড্যাশ/স্পেস ছাড়া

    private fun contactIconButton(kind: ContactIcon.Kind, onTap: () -> Unit): View {
        val icon = ContactIcon(this, kind)
        icon.setOnClickListener { onTap() }
        return icon
    }

    private fun contactRow(): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val size = dp(42)

        row.addView(
            contactIconButton(ContactIcon.Kind.FACEBOOK) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl)))
                } catch (e: Exception) {
                    Toast.makeText(this, "লিংক খোলা যায়নি", Toast.LENGTH_SHORT).show()
                }
            },
            lp(size, size, 0, 0, dp(14), 0)
        )
        row.addView(
            contactIconButton(ContactIcon.Kind.WHATSAPP) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsappNumber")))
                } catch (e: Exception) {
                    Toast.makeText(this, "WhatsApp খোলা যায়নি", Toast.LENGTH_SHORT).show()
                }
            },
            lp(size, size, 0, 0, dp(14), 0)
        )
        row.addView(
            contactIconButton(ContactIcon.Kind.CALL) {
                try {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
                } catch (e: Exception) {
                    Toast.makeText(this, "ডায়াল খোলা যায়নি", Toast.LENGTH_SHORT).show()
                }
            },
            lp(size, size)
        )

        row.addView(
            PillLabel(this, phoneNumber, white(140), p.text),
            lp(WRAP_CONTENT, WRAP_CONTENT, dp(12), 0, 0, 0)
        )
        return row
    }

    private fun footer(): View {
        val rad = dpf(32f)
        val root = FrameLayout(this)
        root.addView(GlassBackdrop(this, rad), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val glassCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = glass(dpf(26f), true)
            setPadding(dp(16), dp(24), dp(16), dp(22))
        }

        // ছবির দুই পাশে সরু রেখা
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(hairline(), lp(0, dp(1), 0, 0, dp(12), 0).apply { weight = 1f })
        top.addView(avatar(), lp(WRAP_CONTENT, WRAP_CONTENT))
        top.addView(hairline(), lp(0, dp(1), dp(12), 0, 0, 0).apply { weight = 1f })
        glassCard.addView(top, lp(MATCH_PARENT, WRAP_CONTENT))

        // Designed & Developed by
        glassCard.addView(
            label("Designed & Developed by", 13.5f, p.sub, true).apply {
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(7), dp(16), dp(7))
                background = glass(dpf(20f))
            },
            lp(WRAP_CONTENT, WRAP_CONTENT, 0, dp(16))
        )

        // </>  Adnan Sami
        val nameRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(label("</>", 22f, p.text, true))
        nameRow.addView(label("Adnan Sami", 27f, p.text, true), lp(WRAP_CONTENT, WRAP_CONTENT, dp(10)))
        glassCard.addView(nameRow, lp(WRAP_CONTENT, WRAP_CONTENT, 0, dp(12)))

        // ─── Software Developer & Creator ───
        val role = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        role.addView(hairline(), lp(0, dp(1)).apply { weight = 1f })
        role.addView(label("Software Developer & Creator", 14f, p.sub), lp(WRAP_CONTENT, WRAP_CONTENT, dp(10), 0, dp(10), 0))
        role.addView(hairline(), lp(0, dp(1)).apply { weight = 1f })
        glassCard.addView(role, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(10)))

        // Thank you for using my app 🖤
        val thanks = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        thanks.addView(label("Thank you for using my app", 14f, p.text).apply {
            setTypeface(Typeface.DEFAULT, Typeface.ITALIC)
        })
        val heart = label("🖤", 16f, p.text)
        thanks.addView(heart, lp(WRAP_CONTENT, WRAP_CONTENT, dp(8)))
        glassCard.addView(thanks, lp(WRAP_CONTENT, WRAP_CONTENT, 0, dp(16)))

        heartAnim = ObjectAnimator.ofPropertyValuesHolder(
            heart,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.3f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.3f)
        ).apply {
            duration = 700
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }

        glassCard.addView(contactRow(), lp(WRAP_CONTENT, WRAP_CONTENT, 0, dp(2), 0, dp(16)))

        // © 2026 • সর্বস্বত্ব সংরক্ষিত
        glassCard.addView(
            label("© 2026  •  সর্বস্বত্ব সংরক্ষিত", 13f, p.text).apply {
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(8), dp(18), dp(8))
                background = glass(dpf(20f))
            },
            lp(WRAP_CONTENT, WRAP_CONTENT, 0, dp(16))
        )

        root.addView(glassCard, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(dp(14), dp(14), dp(14), dp(14))
        })
        return root
    }

    override fun onDestroy() {
        heartAnim?.cancel()
        super.onDestroy()
    }

    private fun header(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = glass(dpf(30f), true)
            elevation = dpf(5f)
            setPadding(dp(22), dp(22), dp(22), dp(22))
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(KeyboardLogo(this), lp(dp(52), dp(52)))
        val pill = label("স্বাগতম  •  ${greeting()}", 13f, p.text, true).apply {
            setPadding(dp(14), dp(7), dp(14), dp(7))
            background = glass(dpf(30f))
        }
        top.addView(pill, lp(WRAP_CONTENT, WRAP_CONTENT, dp(12)))
        box.addView(top)

        box.addView(label("বিজয় ফিজিক্যাল\nকিবোর্ড", 32f, p.text, true), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(20)))
        box.addView(
            label(
                "OTG বা যেকোনো কিবোর্ডে ইংরেজি কী চাপুন, যেকোনো অ্যাপে নিখুঁত বাংলা পান। কম্পিউটারের বিজয় বাহান্ন (৫২) লেআউটের মতোই।",
                14.5f, p.sub
            ), lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(10))
        )

        val feats = LinearLayout(this)
        for ((i, f) in listOf("বিজয় ৫২", "যেকোনো অ্যাপে", "অফলাইন").withIndex()) {
            val chip = label(f, 12f, p.text, true).apply {
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = rounded(p.key, dpf(20f), p.stroke, dp(1))
            }
            feats.addView(chip, lp(WRAP_CONTENT, WRAP_CONTENT, if (i == 0) 0 else dp(8)))
        }
        box.addView(feats, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(18)))
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

    // ---------- লাইসেন্স (paid ফ্লেভার) ----------
    private fun licenseCard(): View {
        val c = card()
        val licensed = License.isLicensed(this)

        c.addView(
            label(if (licensed) "লাইসেন্স সক্রিয় ✓" else "লাইসেন্স প্রয়োজন", 18f, if (licensed) p.ok else p.text, true),
            lp(MATCH_PARENT, WRAP_CONTENT)
        )

        if (licensed) {
            c.addView(
                label("এই ডিভাইসের জন্য সম্পূর্ণ সংস্করণ সক্রিয় আছে।", 13.5f, p.sub),
                lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(6))
            )
            return c
        }

        c.addView(
            label("নিচের ডিভাইস আইডিটি পাঠিয়ে লাইসেন্স কোড সংগ্রহ করুন, তারপর এখানে বসান:", 13.5f, p.sub),
            lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(6))
        )

        val idBox = label(License.deviceId(this), 14f, p.text, true).apply {
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = glass(dpf(16f))
            isClickable = true
            setOnClickListener {
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("device id", License.deviceId(this@MainActivity)))
                Toast.makeText(this@MainActivity, "ডিভাইস আইডি কপি হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
        c.addView(idBox, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(10)))

        val input = EditText(this).apply {
            hint = "লাইসেন্স কোড এখানে পেস্ট করুন"
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = glass(dpf(16f))
            setTextColor(p.text)
            isSingleLine = false
            maxLines = 4
        }
        c.addView(input, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(12)))

        val activate = label("সক্রিয় করুন", 15f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpf(16f)
                setColor(p.primary)
            }
            isClickable = true
            setOnClickListener {
                val code = input.text?.toString().orEmpty()
                if (code.isBlank()) {
                    Toast.makeText(this@MainActivity, "কোডটি বসান", Toast.LENGTH_SHORT).show()
                } else if (License.activate(this@MainActivity, code)) {
                    Toast.makeText(this@MainActivity, "লাইসেন্স সক্রিয় হয়েছে ✓", Toast.LENGTH_SHORT).show()
                    recreate()
                } else {
                    Toast.makeText(this@MainActivity, "কোডটি সঠিক নয় / এই ডিভাইসের জন্য নয়", Toast.LENGTH_LONG).show()
                }
            }
        }
        c.addView(activate, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(2)))
        return c
    }

    // ---------- ট্রায়াল সময় (trial ফ্লেভার) ----------
    private lateinit var trialSub: TextView
    private val trialHandler = Handler(Looper.getMainLooper())
    private val trialTick = object : Runnable {
        override fun run() {
            if (::trialSub.isInitialized) {
                val ms = Trial.remainingMs(this@MainActivity)
                val m = ms / 60000
                val s = (ms % 60000) / 1000
                trialSub.text = if (ms > 0)
                    "বাকি সময়: %02d:%02d".format(m, s)
                else
                    "ট্রায়ালের সময় শেষ হয়ে গেছে"
                trialSub.setTextColor(if (ms > 0) p.sub else p.text)
            }
            trialHandler.postDelayed(this, 1000)
        }
    }

    private fun trialCard(): View {
        val c = card()
        c.addView(label("ট্রায়াল সংস্করণ", 18f, p.text, true), lp(MATCH_PARENT, WRAP_CONTENT))
        trialSub = label("", 14f, p.sub)
        c.addView(trialSub, lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(6)))
        c.addView(
            label("কিবোর্ড চালু রেখে টাইপ করা অবস্থায় মোট ৩ মিনিট ব্যবহার করা যাবে।", 12.5f, p.sub),
            lp(MATCH_PARENT, WRAP_CONTENT, 0, dp(8))
        )
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
        if (BuildConfig.IS_PAID && !License.isLicensed(this)) {
            Toast.makeText(this, "আগে উপরে লাইসেন্স কোডটি বসিয়ে সক্রিয় করুন", Toast.LENGTH_LONG).show()
            return
        }
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
        if (!BuildConfig.IS_PAID) trialHandler.post(trialTick)
        if (waiting && enabled() && !selected()) {
            waiting = false
            toggle.postDelayed({ imm.showInputMethodPicker() }, 400)
        }
    }

    override fun onPause() {
        trialHandler.removeCallbacks(trialTick)
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refresh()
    }

    private fun refresh() {
        // লাইসেন্স পেজে মূল স্ক্রিনের ভিউ (toggle ইত্যাদি) তৈরি হয় না — তখন কিছু করার নেই
        if (!::toggle.isInitialized || !::statusTitle.isInitialized || !::statusSub.isInitialized) return
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
