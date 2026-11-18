package com.feng.fengos

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Setup awal ala iPhone: Hello -> Bahasa -> Wi-Fi -> Passcode -> Feng ID -> Welcome.
 * Passcode di sini kosmetik (tidak mengunci HP). Keamanan tetap dari kunci layar Android.
 */
class SetupView(
    c: Context,
    private val topInset: Int,
    private val botInset: Int,
    private val onFinish: () -> Unit
) : FrameLayout(c) {

    private val prefs = c.getSharedPreferences("fengos", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var cur: View? = null

    init {
        setBackgroundColor(Color.BLACK)
        swap(hello(), false)
    }

    /** Dipanggil Activity saat keyboard muncul agar tombol tidak tertutup. */
    fun setImeInset(px: Int) {
        setPadding(0, 0, 0, px)
    }

    private fun swap(next: View, anim: Boolean = true) {
        val old = cur
        cur = next
        addView(next, LayoutParams(MATCH, MATCH))
        if (old == null) return
        if (!anim) {
            removeView(old)
            return
        }
        val w = width.toFloat().coerceAtLeast(1f)
        next.translationX = w
        next.animate().translationX(0f).setDuration(380).start()
        old.animate().translationX(-w * 0.3f).alpha(0.3f).setDuration(380)
            .withEndAction { removeView(old) }.start()
    }

    // ---------- 1. Hello ----------
    private fun hello(): View {
        val c = context
        val root = FrameLayout(c)
        root.setBackgroundColor(Color.BLACK)

        val tv = c.label("hello", 52f, Color.WHITE, true, Gravity.CENTER)
        root.addView(tv, LayoutParams(MATCH, WRAP, Gravity.CENTER))

        val hint = c.label("swipe up to get started", 17f, cw(140), false, Gravity.CENTER)
        val hintLp = LayoutParams(MATCH, WRAP, Gravity.BOTTOM)
        hintLp.bottomMargin = botInset + c.dpi(48f)
        root.addView(hint, hintLp)

        val bar = View(c)
        val barBg = GradientDrawable()
        barBg.setColor(cw(230))
        barBg.cornerRadius = c.dp(3f)
        bar.background = barBg
        val barLp = LayoutParams(c.dpi(134f), c.dpi(5f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        barLp.bottomMargin = botInset + c.dpi(8f)
        root.addView(bar, barLp)

        val words = listOf(
            "hello", "halo", "你好", "hola", "こんにちは", "bonjour",
            "안녕하세요", "привет", "नमस्ते", "hallo", "ciao", "مرحبا"
        )
        val colors = intArrayOf(
            0xFFFF9F0A.toInt(), 0xFFFF375F.toInt(), 0xFFBF5AF2.toInt(), 0xFF0A84FF.toInt()
        )
        var i = 0

        fun paintGradient() {
            val tw = tv.paint.measureText(tv.text.toString())
            val x0 = (tv.width - tw) / 2f
            tv.paint.shader = LinearGradient(x0, 0f, x0 + tw, 0f, colors, null, Shader.TileMode.CLAMP)
            tv.invalidate()
        }

        val step = object : Runnable {
            override fun run() {
                tv.animate().alpha(0f).setDuration(350).withEndAction {
                    i = (i + 1) % words.size
                    tv.text = words[i]
                    paintGradient()
                    tv.animate().alpha(1f).setDuration(350).start()
                }.start()
                handler.postDelayed(this, 2000)
            }
        }
        tv.post { paintGradient() }
        handler.postDelayed(step, 1800)

        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                handler.removeCallbacksAndMessages(null)
            }
        })

        var downY = 0f
        root.setOnTouchListener { _, e ->
            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                downY = e.y
            } else if (e.actionMasked == MotionEvent.ACTION_UP) {
                if (downY - e.y > c.dp(80f)) swap(language())
            }
            true
        }
        hint.setOnClickListener { swap(language()) }
        return root
    }

    // ---------- helpers ----------
    private fun lb(s: String, sp: Float, color: Int = Color.WHITE, bold: Boolean = false, g: Int = Gravity.START): TextView =
        context.label(s, sp, color, bold, g)

    private fun button(t: String, filled: Boolean, click: () -> Unit): TextView {
        val b = lb(t, 17f, if (filled) Color.WHITE else U.BLUE, filled, Gravity.CENTER)
        if (filled) {
            val bg = GradientDrawable()
            bg.setColor(U.BLUE)
            bg.cornerRadius = context.dp(14f)
            b.background = bg
        }
        b.setOnTouchListener { v, e ->
            if (e.actionMasked == MotionEvent.ACTION_DOWN) v.alpha = 0.6f
            if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) v.alpha = 1f
            false
        }
        b.setOnClickListener { click() }
        return b
    }

    private fun page(
        title: String,
        sub: String?,
        body: View?,
        primary: String?,
        secondary: String? = null,
        onPrimary: () -> Unit = {},
        onSecondary: () -> Unit = {}
    ): LinearLayout {
        val c = context
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.setBackgroundColor(Color.BLACK)
        col.setPadding(c.dpi(24f), topInset + c.dpi(56f), c.dpi(24f), botInset + c.dpi(20f))

        col.addView(lb(title, 30f, Color.WHITE, true, Gravity.CENTER), LinearLayout.LayoutParams(MATCH, WRAP))
        if (sub != null) {
            val s = lb(sub, 16f, cw(150), false, Gravity.CENTER)
            s.setLineSpacing(0f, 1.15f)
            val lp = LinearLayout.LayoutParams(MATCH, WRAP)
            lp.topMargin = c.dpi(12f)
            col.addView(s, lp)
        }
        val bodyLp = LinearLayout.LayoutParams(MATCH, 0, 1f)
        bodyLp.topMargin = c.dpi(24f)
        bodyLp.bottomMargin = c.dpi(16f)
        col.addView(body ?: View(c), bodyLp)

        if (primary != null) {
            col.addView(button(primary, true, onPrimary), LinearLayout.LayoutParams(MATCH, c.dpi(54f)))
        }
        if (secondary != null) {
            val lp = LinearLayout.LayoutParams(MATCH, c.dpi(48f))
            lp.topMargin = c.dpi(6f)
            col.addView(button(secondary, false, onSecondary), lp)
        }
        return col
    }

    private fun dark(radius: Float): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(Color.rgb(28, 28, 30))
        g.cornerRadius = context.dp(radius)
        return g
    }

    // ---------- 2. Bahasa ----------
    private fun language(): View {
        val c = context
        val langs = listOf(
            "English", "Bahasa Indonesia", "中文 (简体)", "日本語", "한국어",
            "Español", "Français", "Deutsch", "العربية", "हिन्दी"
        )
        var sel = prefs.getInt("lang", 0)
        val checks = mutableListOf<TextView>()
        val list = LinearLayout(c)
        list.orientation = LinearLayout.VERTICAL
        list.background = dark(12f)

        langs.forEachIndexed { i, name ->
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(c.dpi(16f), 0, c.dpi(16f), 0)
            row.addView(lb(name, 17f), LinearLayout.LayoutParams(0, WRAP, 1f))
            val chk = lb("✓", 18f, U.BLUE, true)
            chk.visibility = if (i == sel) View.VISIBLE else View.INVISIBLE
            row.addView(chk)
            checks.add(chk)
            row.setOnClickListener {
                sel = i
                checks.forEachIndexed { j, t -> t.visibility = if (j == i) View.VISIBLE else View.INVISIBLE }
                prefs.edit().putInt("lang", i).apply()
            }
            list.addView(row, LinearLayout.LayoutParams(MATCH, c.dpi(52f)))
            if (i < langs.size - 1) {
                val sep = View(c)
                sep.setBackgroundColor(cw(30))
                val lp = LinearLayout.LayoutParams(MATCH, 1)
                lp.leftMargin = c.dpi(16f)
                list.addView(sep, lp)
            }
        }
        val sv = ScrollView(c)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(list, LayoutParams(MATCH, WRAP))
        return page("Language", "Choose your language", sv, "Continue", null, { swap(wifi()) })
    }

    // ---------- 3. Wi-Fi ----------
    private fun wifi(): View {
        val c = context
        val body = LinearLayout(c)
        body.orientation = LinearLayout.VERTICAL
        body.gravity = Gravity.CENTER
        body.addView(lb("Wi-Fi", 64f, Color.WHITE, true, Gravity.CENTER))
        val open = button("Open Wi-Fi Settings", false) {
            try {
                c.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
            }
        }
        val lp = LinearLayout.LayoutParams(MATCH, c.dpi(48f))
        lp.topMargin = c.dpi(24f)
        body.addView(open, lp)
        return page(
            "Choose a Wi-Fi Network",
            "Connect to a Wi-Fi network to continue setting up your device.",
            body, "Continue", null, { swap(passcode()) }
        )
    }

    // ---------- 4. Passcode (kosmetik) ----------
    private fun passcode(): View {
        val c = context
        var first: String? = null
        val entry = StringBuilder()
        var titleTv: TextView? = null

        val dots = LinearLayout(c)
        dots.orientation = LinearLayout.HORIZONTAL
        dots.gravity = Gravity.CENTER
        val dotBgs = ArrayList<GradientDrawable>()
        for (k in 0 until 6) {
            val v = View(c)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setStroke(c.dpi(1.5f), Color.WHITE)
            g.setColor(Color.TRANSPARENT)
            v.background = g
            dotBgs.add(g)
            val lp = LinearLayout.LayoutParams(c.dpi(14f), c.dpi(14f))
            lp.leftMargin = c.dpi(9f)
            lp.rightMargin = c.dpi(9f)
            dots.addView(v, lp)
        }

        fun refreshDots() {
            for (k in 0 until 6) dotBgs[k].setColor(if (k < entry.length) Color.WHITE else Color.TRANSPARENT)
        }

        fun onDigit(d: String) {
            if (d == "⌫") {
                if (entry.isNotEmpty()) entry.deleteCharAt(entry.length - 1)
                refreshDots()
                return
            }
            if (entry.length >= 6) return
            entry.append(d)
            refreshDots()
            if (entry.length == 6) {
                handler.postDelayed({
                    val f = first
                    if (f == null) {
                        first = entry.toString()
                        entry.setLength(0)
                        titleTv?.text = "Re-enter your Passcode"
                        refreshDots()
                    } else if (f == entry.toString()) {
                        swap(fengId())
                    } else {
                        first = null
                        entry.setLength(0)
                        titleTv?.text = "Passcodes Did Not Match"
                        ObjectAnimator.ofFloat(dots, View.TRANSLATION_X, 0f, 24f, -24f, 16f, -16f, 0f)
                            .setDuration(400).start()
                        refreshDots()
                    }
                }, 150)
            }
        }

        val body = LinearLayout(c)
        body.orientation = LinearLayout.VERTICAL
        body.gravity = Gravity.CENTER_HORIZONTAL
        val dlp = LinearLayout.LayoutParams(WRAP, WRAP)
        dlp.bottomMargin = c.dpi(36f)
        body.addView(dots, dlp)

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        for (r in 0 until 4) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            for (k in 0 until 3) {
                val key = keys[r * 3 + k]
                val size = c.dpi(74f)
                val lp = LinearLayout.LayoutParams(size, size)
                lp.leftMargin = c.dpi(10f)
                lp.rightMargin = c.dpi(10f)
                lp.bottomMargin = c.dpi(12f)
                if (key.isEmpty()) {
                    row.addView(View(c), lp)
                } else {
                    val b = lb(key, if (key == "⌫") 24f else 30f, Color.WHITE, false, Gravity.CENTER)
                    if (key != "⌫") {
                        val bg = GradientDrawable()
                        bg.shape = GradientDrawable.OVAL
                        bg.setColor(cw(45))
                        b.background = bg
                    }
                    b.setOnTouchListener { v, e ->
                        if (e.actionMasked == MotionEvent.ACTION_DOWN) v.alpha = 0.5f
                        if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) v.alpha = 1f
                        false
                    }
                    b.setOnClickListener { onDigit(key) }
                    row.addView(b, lp)
                }
            }
            body.addView(row)
        }

        val p = page(
            "Create a Passcode",
            "A passcode is used to unlock your device.",
            body, null, "Set Up Later", {}, { swap(fengId()) }
        )
        titleTv = p.getChildAt(0) as TextView
        return p
    }

    // ---------- 5. Feng ID ----------
    private fun fengId(): View {
        val c = context
        val et = EditText(c)
        et.hint = "Your name"
        et.setHintTextColor(cw(110))
        et.setTextColor(Color.WHITE)
        et.textSize = 18f
        et.typeface = U.tf
        et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        et.isSingleLine = true
        et.background = dark(12f)
        et.setPadding(c.dpi(16f), c.dpi(14f), c.dpi(16f), c.dpi(14f))
        et.setText(prefs.getString("name", ""))

        val body = LinearLayout(c)
        body.orientation = LinearLayout.VERTICAL
        body.addView(et, LinearLayout.LayoutParams(MATCH, WRAP))

        return page(
            "Sign In with Feng ID",
            "Enter your name to personalize your device.",
            body, "Continue", "Set Up Later in Settings",
            {
                prefs.edit().putString("name", et.text.toString().trim()).apply()
                swap(welcome())
            },
            { swap(welcome()) }
        )
    }

    // ---------- 6. Welcome ----------
    private fun welcome(): View {
        val c = context
        val body = LinearLayout(c)
        body.orientation = LinearLayout.VERTICAL
        body.gravity = Gravity.CENTER
        body.addView(lb("F", 96f, Color.WHITE, true, Gravity.CENTER))
        return page(
            "Welcome to FengOS",
            "Everything is ready.",
            body, "Get Started", null, { onFinish() }
        )
    }
}
