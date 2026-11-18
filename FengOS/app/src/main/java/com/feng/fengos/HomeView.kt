package com.feng.fengos

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView

// ======================= Pager halaman =======================

class Pager(c: Context, private val pageW: Int) : HorizontalScrollView(c) {
    var onPage: ((Int) -> Unit)? = null
    var pages = 1
    private var downX = 0f
    private var startPage = 0

    init {
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
    }

    private fun cur(): Int = Math.round(scrollX / pageW.toFloat())

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = e.x
            startPage = cur()
        }
        return super.onInterceptTouchEvent(e)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = e.x
            startPage = cur()
        }
        val r = super.onTouchEvent(e)
        if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) {
            val dx = downX - e.x
            val th = pageW * 0.10f
            val step = if (dx > th) 1 else if (dx < -th) -1 else 0
            val t = (startPage + step).coerceIn(0, pages - 1)
            smoothScrollTo(t * pageW, 0)
        }
        return r
    }

    fun goTo(p: Int, anim: Boolean) {
        if (anim) smoothScrollTo(p * pageW, 0) else scrollTo(p * pageW, 0)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onPage?.invoke(Math.round(l / pageW.toFloat()))
    }
}

// ======================= Widget baterai =======================

class BatteryRing(c: Context) : View(c) {
    var pct = 100
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat()
        val sw = s * 0.12f
        val left = (width - s) / 2f
        val top = (height - s) / 2f
        val r = RectF(left + sw / 2, top + sw / 2, left + s - sw / 2, top + s - sw / 2)
        p.style = Paint.Style.STROKE
        p.strokeWidth = sw
        p.strokeCap = Paint.Cap.ROUND
        p.color = cw(50)
        canvas.drawArc(r, 0f, 360f, false, p)
        p.color = if (pct <= 20) Color.rgb(255, 69, 58) else Color.rgb(48, 209, 88)
        canvas.drawArc(r, -90f, 360f * pct / 100f, false, p)
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        p.typeface = U.tfBold
        p.textAlign = Paint.Align.CENTER
        p.textSize = s * 0.26f
        canvas.drawText("$pct%", width / 2f, height / 2f + p.textSize * 0.36f, p)
    }
}

// ======================= Spotlight =======================

class SpotlightView(
    c: Context,
    private val apps: List<AppInfo>,
    topInset: Int,
    private val launch: (AppInfo) -> Unit,
    private val onClose: () -> Unit
) : FrameLayout(c) {
    private val list = LinearLayout(c)
    private val et = EditText(c)
    private val imm = c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    init {
        setBackgroundColor(cb(200))
        visibility = GONE
        isClickable = true

        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(c.dpi(16f), topInset + c.dpi(12f), c.dpi(16f), 0)

        val bar = LinearLayout(c)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL

        et.hint = "Search"
        et.setHintTextColor(cw(120))
        et.setTextColor(Color.WHITE)
        et.textSize = 17f
        et.typeface = U.tf
        et.isSingleLine = true
        et.background = c.glass(12f, cw(40), cw(0))
        et.setPadding(c.dpi(14f), c.dpi(10f), c.dpi(14f), c.dpi(10f))
        et.setCompoundDrawablesRelativeWithIntrinsicBounds(c.getDrawable(R.drawable.ic_search), null, null, null)
        et.compoundDrawablePadding = c.dpi(8f)
        bar.addView(et, LinearLayout.LayoutParams(0, WRAP, 1f))

        val cancel = c.label("Cancel", 17f, U.BLUE)
        cancel.setPadding(c.dpi(14f), c.dpi(10f), 0, c.dpi(10f))
        cancel.setOnClickListener { close() }
        bar.addView(cancel)
        col.addView(bar, LinearLayout.LayoutParams(MATCH, WRAP))

        val sv = ScrollView(c)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(list, LayoutParams(MATCH, WRAP))
        val svLp = LinearLayout.LayoutParams(MATCH, 0, 1f)
        svLp.topMargin = c.dpi(12f)
        col.addView(sv, svLp)
        addView(col, LayoutParams(MATCH, MATCH))

        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, cnt: Int) {}
            override fun afterTextChanged(s: Editable?) {
                refresh(s?.toString() ?: "")
            }
        })
    }

    private fun refresh(q: String) {
        list.removeAllViews()
        val c = context
        val res = if (q.isBlank()) apps.take(8) else apps.filter { it.label.contains(q, true) }.take(30)
        if (q.isBlank()) {
            val h = c.label("Suggestions", 13f, cw(150), true)
            h.setPadding(c.dpi(4f), c.dpi(8f), 0, c.dpi(8f))
            list.addView(h)
        }
        for (a in res) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(c.dpi(4f), c.dpi(8f), c.dpi(4f), c.dpi(8f))
            row.addView(SquircleIcon(c, a.icon, c.dpi(44f)))
            val t = c.label(a.label, 17f)
            t.setPadding(c.dpi(14f), 0, 0, 0)
            row.addView(t)
            row.setOnClickListener {
                close()
                launch(a)
            }
            list.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
        }
    }

    fun open() {
        visibility = VISIBLE
        alpha = 0f
        animate().alpha(1f).setDuration(200).start()
        et.setText("")
        refresh("")
        et.requestFocus()
        et.post { imm.showSoftInput(et, 0) }
    }

    fun close() {
        imm.hideSoftInputFromWindow(et.windowToken, 0)
        animate().alpha(0f).setDuration(160).withEndAction { visibility = GONE }.start()
        onClose()
    }

    val isOpen: Boolean get() = visibility == VISIBLE
}

// ======================= Home screen =======================

class HomeView(c: Context, private val topInset: Int, private val botInset: Int) : FrameLayout(c) {

    private val sw = c.resources.displayMetrics.widthPixels
    private val apps = loadApps(c)
    private val iconS = (sw / 4f * 0.62f).toInt()
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = c.getSharedPreferences("fengos", Context.MODE_PRIVATE)

    private val cells = ArrayList<View>()
    private val anims = ArrayList<ObjectAnimator>()
    private var jiggling = false

    private val pager = Pager(c, sw)
    private val dots = LinearLayout(c)
    private lateinit var doneBtn: TextView
    private lateinit var spotlight: SpotlightView
    private val ring = BatteryRing(c)

    private val batteryTick = object : Runnable {
        override fun run() {
            updateBattery()
            handler.postDelayed(this, 30000)
        }
    }

    init {
        // wallpaper: taruh wallpaper.jpg di assets untuk menggantinya
        val wp = ImageView(c)
        wp.scaleType = ImageView.ScaleType.CENTER_CROP
        var loaded = false
        try {
            c.assets.open("wallpaper.jpg").use { st ->
                val opt = BitmapFactory.Options()
                opt.inSampleSize = 1
                val bmp = BitmapFactory.decodeStream(st, null, opt)
                if (bmp != null) {
                    wp.setImageBitmap(bmp)
                    loaded = true
                }
            }
        } catch (e: Exception) {
        }
        if (!loaded) {
            wp.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(10, 24, 74), Color.rgb(75, 43, 181),
                    Color.rgb(214, 71, 155), Color.rgb(255, 154, 90)
                )
            )
        }
        addView(wp, LayoutParams(MATCH, MATCH))

        // dock
        val dock = dockApps()
        val grid = apps.filter { a -> dock.none { d -> d.pkg == a.pkg } }

        val pageViews = ArrayList<View>()
        pageViews.add(gridPage(grid.take(16), 4, true))
        var rest = grid.drop(16)
        while (rest.isNotEmpty()) {
            pageViews.add(gridPage(rest.take(24), 6, false))
            rest = rest.drop(24)
        }
        pageViews.add(libraryPage())

        val strip = LinearLayout(c)
        strip.orientation = LinearLayout.HORIZONTAL
        for (pv in pageViews) strip.addView(pv, LinearLayout.LayoutParams(sw, MATCH))
        pager.addView(strip, LayoutParams(WRAP, MATCH))
        pager.pages = pageViews.size

        // titik halaman
        dots.orientation = LinearLayout.HORIZONTAL
        dots.gravity = Gravity.CENTER
        for (i in 0 until pageViews.size) {
            val d = View(c)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(Color.WHITE)
            d.background = g
            val lp = LinearLayout.LayoutParams(c.dpi(7f), c.dpi(7f))
            lp.leftMargin = c.dpi(4f)
            lp.rightMargin = c.dpi(4f)
            dots.addView(d, lp)
        }
        setDots(0)
        pager.onPage = { p -> setDots(p) }

        // pill pencarian
        val pill = c.label("Search", 14f, cw(230), false, Gravity.CENTER)
        pill.background = c.glass(18f, cw(55), cw(70))
        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(c.getDrawable(R.drawable.ic_search), null, null, null)
        pill.compoundDrawablePadding = c.dpi(6f)
        pill.setPadding(c.dpi(18f), 0, c.dpi(18f), 0)
        pill.setOnClickListener { spotlight.open() }

        // dock
        val dockBar = LinearLayout(c)
        dockBar.orientation = LinearLayout.HORIZONTAL
        dockBar.gravity = Gravity.CENTER_VERTICAL
        dockBar.background = c.glass(34f, cw(60), cw(80))
        dockBar.setPadding(0, c.dpi(12f), 0, c.dpi(12f))
        for (a in dock) {
            val cell = FrameLayout(c)
            val ic = SquircleIcon(c, a.icon, iconS)
            cell.addView(ic, LayoutParams(WRAP, WRAP, Gravity.CENTER))
            wireCell(cell, a)
            dockBar.addView(cell, LinearLayout.LayoutParams(0, WRAP, 1f))
        }

        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.addView(pager, LinearLayout.LayoutParams(MATCH, 0, 1f))
        val dotsLp = LinearLayout.LayoutParams(MATCH, WRAP)
        dotsLp.topMargin = c.dpi(6f)
        dotsLp.bottomMargin = c.dpi(10f)
        col.addView(dots, dotsLp)
        val pillLp = LinearLayout.LayoutParams(WRAP, c.dpi(36f))
        pillLp.gravity = Gravity.CENTER_HORIZONTAL
        pillLp.bottomMargin = c.dpi(12f)
        col.addView(pill, pillLp)
        val dockLp = LinearLayout.LayoutParams(MATCH, WRAP)
        dockLp.leftMargin = c.dpi(12f)
        dockLp.rightMargin = c.dpi(12f)
        dockLp.bottomMargin = botInset + c.dpi(8f)
        col.addView(dockBar, dockLp)
        addView(col, LayoutParams(MATCH, MATCH))

        // tombol Done (mode goyang)
        doneBtn = c.label("Done", 15f, Color.WHITE, true, Gravity.CENTER)
        doneBtn.background = c.glass(16f, cw(70), cw(90))
        doneBtn.setPadding(c.dpi(16f), c.dpi(7f), c.dpi(16f), c.dpi(7f))
        doneBtn.visibility = GONE
        doneBtn.setOnClickListener { stopJiggle() }
        val doneLp = LayoutParams(WRAP, WRAP, Gravity.TOP or Gravity.END)
        doneLp.topMargin = topInset + c.dpi(8f)
        doneLp.rightMargin = c.dpi(16f)
        addView(doneBtn, doneLp)

        // spotlight (paling atas)
        spotlight = SpotlightView(c, apps, topInset, { a -> open(a) }, {})
        addView(spotlight, LayoutParams(MATCH, MATCH))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(batteryTick)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(batteryTick)
    }

    // ---------- aksi ----------
    private fun open(a: AppInfo) {
        try {
            context.startActivity(a.intent)
        } catch (e: Exception) {
        }
    }

    private fun setDots(p: Int) {
        for (i in 0 until dots.childCount) dots.getChildAt(i).alpha = if (i == p) 1f else 0.35f
    }

    private fun updateBattery() {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0 && scale > 0) {
            ring.pct = level * 100 / scale
            ring.invalidate()
        }
    }

    fun onHomePressed() {
        if (spotlight.isOpen) spotlight.close()
        if (jiggling) stopJiggle()
        pager.goTo(0, true)
    }

    /** true jika event "back" sudah dipakai. */
    fun onBack(): Boolean {
        if (spotlight.isOpen) {
            spotlight.close()
            return true
        }
        if (jiggling) {
            stopJiggle()
            return true
        }
        return false
    }

    // ---------- mode goyang ----------
    private fun startJiggle() {
        if (jiggling) return
        jiggling = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        for (v in cells) {
            val a = ObjectAnimator.ofFloat(v, View.ROTATION, -2f, 2f)
            a.duration = 100L + (Math.random() * 40).toLong()
            a.repeatMode = ValueAnimator.REVERSE
            a.repeatCount = ValueAnimator.INFINITE
            a.start()
            anims.add(a)
        }
        doneBtn.visibility = VISIBLE
    }

    private fun stopJiggle() {
        jiggling = false
        for (a in anims) a.cancel()
        anims.clear()
        for (v in cells) v.rotation = 0f
        doneBtn.visibility = GONE
    }

    private fun wireCell(v: View, a: AppInfo) {
        v.setOnClickListener { if (!jiggling) open(a) }
        v.setOnLongClickListener {
            startJiggle()
            true
        }
        v.setOnTouchListener { view, e ->
            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start()
            } else if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
        cells.add(v)
    }

    // ---------- dock ----------
    private fun dockApps(): List<AppInfo> {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
            Intent("android.media.action.STILL_IMAGE_CAMERA")
        )
        val out = ArrayList<AppInfo>()
        for (intent in intents) {
            val ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: continue
            val pkg = ri.activityInfo.packageName
            if (pkg == "android") continue
            val a = apps.firstOrNull { it.pkg == pkg } ?: continue
            if (out.none { it.pkg == a.pkg }) out.add(a)
        }
        for (a in apps) {
            if (out.size >= 4) break
            if (out.none { it.pkg == a.pkg }) out.add(a)
        }
        return out.take(4)
    }

    // ---------- halaman ikon ----------
    private fun gridPage(list: List<AppInfo>, rows: Int, withWidgets: Boolean): View {
        val c = context
        val page = LinearLayout(c)
        page.orientation = LinearLayout.VERTICAL
        page.setPadding(0, topInset + c.dpi(8f), 0, 0)
        if (withWidgets) page.addView(widgetRow(), LinearLayout.LayoutParams(MATCH, 0, 2f))
        var idx = 0
        for (r in 0 until rows) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            for (k in 0 until 4) {
                val a = list.getOrNull(idx++)
                row.addView(if (a != null) cell(a) else View(c), LinearLayout.LayoutParams(0, MATCH, 1f))
            }
            page.addView(row, LinearLayout.LayoutParams(MATCH, 0, 1f))
        }
        return page
    }

    private fun cell(a: AppInfo): View {
        val c = context
        val v = LinearLayout(c)
        v.orientation = LinearLayout.VERTICAL
        v.gravity = Gravity.CENTER_HORIZONTAL
        v.setPadding(0, c.dpi(6f), 0, 0)
        v.addView(SquircleIcon(c, a.icon, iconS))
        val t = c.label(a.label, 11f, Color.WHITE, false, Gravity.CENTER)
        t.maxLines = 1
        t.ellipsize = TextUtils.TruncateAt.END
        t.setShadowLayer(3f, 0f, 1f, cb(100))
        t.setPadding(c.dpi(2f), c.dpi(5f), c.dpi(2f), 0)
        v.addView(t, LinearLayout.LayoutParams(MATCH, WRAP))
        wireCell(v, a)
        return v
    }

    // ---------- widget ----------
    private fun widgetRow(): View {
        val c = context
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(c.dpi(20f), c.dpi(4f), c.dpi(20f), c.dpi(4f))

        // widget jam
        val name = prefs.getString("name", "") ?: ""
        val clock = LinearLayout(c)
        clock.orientation = LinearLayout.VERTICAL
        clock.background = c.glass(26f, cw(55), cw(80))
        clock.setPadding(c.dpi(16f), c.dpi(16f), c.dpi(16f), c.dpi(16f))
        clock.addView(c.label(if (name.isEmpty()) "Today" else "Hi, $name", 13f, cw(200), true))
        val time = TextClock(c)
        time.format12Hour = "h:mm"
        time.format24Hour = "HH:mm"
        time.textSize = 44f
        time.setTextColor(Color.WHITE)
        time.typeface = U.tfBold
        time.includeFontPadding = false
        val tlp = LinearLayout.LayoutParams(WRAP, 0, 1f)
        tlp.gravity = Gravity.CENTER_VERTICAL
        clock.addView(time, tlp)
        val date = TextClock(c)
        date.format12Hour = "EEEE, d MMM"
        date.format24Hour = "EEEE, d MMM"
        date.textSize = 14f
        date.setTextColor(cw(220))
        date.typeface = U.tf
        clock.addView(date)
        val clp = LinearLayout.LayoutParams(0, MATCH, 1f)
        clp.rightMargin = c.dpi(7f)
        row.addView(clock, clp)

        // widget baterai
        val bat = LinearLayout(c)
        bat.orientation = LinearLayout.VERTICAL
        bat.background = c.glass(26f, cw(55), cw(80))
        bat.setPadding(c.dpi(16f), c.dpi(16f), c.dpi(16f), c.dpi(16f))
        bat.addView(c.label("Battery", 13f, cw(200), true))
        val rlp = LinearLayout.LayoutParams(MATCH, 0, 1f)
        rlp.topMargin = c.dpi(8f)
        bat.addView(ring, rlp)
        val blp = LinearLayout.LayoutParams(0, MATCH, 1f)
        blp.leftMargin = c.dpi(7f)
        row.addView(bat, blp)
        return row
    }

    // ---------- App Library ----------
    private fun libraryPage(): View {
        val c = context
        val sv = ScrollView(c)
        sv.isVerticalScrollBarEnabled = false
        sv.overScrollMode = OVER_SCROLL_NEVER
        val m = c.dpi(16f)
        val gap = c.dpi(14f)
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(m, topInset + c.dpi(8f), m, c.dpi(24f))

        val bar = c.label("App Library", 15f, cw(210), false, Gravity.CENTER_VERTICAL)
        bar.background = c.glass(14f, cw(45), cw(60))
        bar.setPadding(c.dpi(14f), 0, c.dpi(14f), 0)
        bar.setCompoundDrawablesRelativeWithIntrinsicBounds(c.getDrawable(R.drawable.ic_search), null, null, null)
        bar.compoundDrawablePadding = c.dpi(8f)
        bar.setOnClickListener { spotlight.open() }
        val barLp = LinearLayout.LayoutParams(MATCH, c.dpi(44f))
        barLp.bottomMargin = c.dpi(16f)
        col.addView(bar, barLp)

        val groups = apps.groupBy { it.cat }.entries.sortedByDescending { it.value.size }.take(8)
        val cardW = (sw - m * 2 - gap) / 2
        var i = 0
        while (i < groups.size) {
            val r = LinearLayout(c)
            r.orientation = LinearLayout.HORIZONTAL
            for (j in 0..1) {
                val g = groups.getOrNull(i + j)
                val lp = LinearLayout.LayoutParams(cardW, WRAP)
                if (j == 0) lp.rightMargin = gap
                r.addView(if (g != null) libCard(g.key, g.value, cardW) else View(c), lp)
            }
            val rlp = LinearLayout.LayoutParams(MATCH, WRAP)
            rlp.bottomMargin = c.dpi(16f)
            col.addView(r, rlp)
            i += 2
        }
        sv.addView(col, LayoutParams(MATCH, WRAP))
        return sv
    }

    private fun libCard(name: String, list: List<AppInfo>, w: Int): View {
        val c = context
        val wrap = LinearLayout(c)
        wrap.orientation = LinearLayout.VERTICAL
        val pad = c.dpi(14f)
        val mini = (w - pad * 3) / 2

        val card = LinearLayout(c)
        card.orientation = LinearLayout.VERTICAL
        card.background = c.glass(26f, cw(48), cw(70))
        card.setPadding(pad, pad, pad, pad)
        for (r in 0..1) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            for (k in 0..1) {
                val a = list.getOrNull(r * 2 + k)
                val v: View = if (a != null) {
                    val ic = SquircleIcon(c, a.icon, mini)
                    ic.setOnClickListener { if (!jiggling) open(a) }
                    ic
                } else View(c)
                val lp = LinearLayout.LayoutParams(mini, mini)
                if (k == 0) lp.rightMargin = pad
                row.addView(v, lp)
            }
            val rlp = LinearLayout.LayoutParams(MATCH, WRAP)
            if (r == 0) rlp.bottomMargin = pad
            card.addView(row, rlp)
        }
        wrap.addView(card, LinearLayout.LayoutParams(w, w))
        val t = c.label(name, 12f, cw(220), false, Gravity.CENTER)
        t.setPadding(0, c.dpi(6f), 0, 0)
        wrap.addView(t, LinearLayout.LayoutParams(w, WRAP))
        return wrap
    }
}
