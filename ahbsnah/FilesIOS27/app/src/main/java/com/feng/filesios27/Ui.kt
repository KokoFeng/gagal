package com.feng.filesios27

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.LruCache
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.EditText
import android.widget.ImageView
import androidx.core.graphics.PathParser
import java.io.File
import java.util.concurrent.Executors

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object C {
    val BLUE = 0xFF007AFF.toInt()
    val BG = 0xFFF2F2F7.toInt()
    val GRAY = 0xFF8E8E93.toInt()
    val RED = 0xFFFF3B30.toInt()
    val tagColors = intArrayOf(
        0xFFFF3B30.toInt(), 0xFFFF9500.toInt(), 0xFFFFCC00.toInt(), 0xFF34C759.toInt(),
        0xFF007AFF.toInt(), 0xFFAF52DE.toInt(), 0xFF8E8E93.toInt()
    )
    val tagNames = arrayOf("Red", "Orange", "Yellow", "Green", "Blue", "Purple", "Gray")
}

/** Kurva animasi ala iOS (cepat di awal, melambat halus). */
val IOS_EASE = PathInterpolator(0.2f, 0.8f, 0.2f, 1f)

fun Context.dp(v: Float) = v * resources.displayMetrics.density
fun Context.dpi(v: Int) = (v * resources.displayMetrics.density).toInt()

fun rr(col: Int, r: Float) = GradientDrawable().apply { setColor(col); cornerRadius = r }
fun oval(col: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(col) }
fun pressBg(): Drawable = StateListDrawable().apply {
    addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(0x1F000000))
    addState(intArrayOf(), ColorDrawable(0))
}

fun EditText.onChange(cb: () -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: Editable?) { cb() }
    })
}

/** Path ikon garis (24x24), gaya SF Symbols. */
object P {
    const val BACK = "M15,5L8,12L15,19"
    const val CHEV = "M9,5L16,12L9,19"
    const val SEARCH = "M10.5,4a6.5,6.5 0 1,0 0,13a6.5,6.5 0 1,0 0,-13 M15.5,15.5L20,20"
    const val MIC = "M12,3a3,3 0 0,0 -3,3v5a3,3 0 0,0 6,0V6a3,3 0 0,0 -3,-3z M6,11a6,6 0 0,0 12,0 M12,17v4"
    const val MORE = "M6,12L6.01,12M12,12L12.01,12M18,12L18.01,12"
    const val COPY = "M8,8h11v12h-11z M5,16V4h11"
    const val FOLDER = "M3,7a2,2 0 0,1 2,-2h4l2,2h8a2,2 0 0,1 2,2v8a2,2 0 0,1 -2,2h-14a2,2 0 0,1 -2,-2z"
    const val NEWFOLDER = "M3,7a2,2 0 0,1 2,-2h4l2,2h8a2,2 0 0,1 2,2v8a2,2 0 0,1 -2,2h-14a2,2 0 0,1 -2,-2z M12,10v5 M9.5,12.5h5"
    const val SHARE = "M12,15V4 M8,8l4,-4l4,4 M6,11H5v9h14v-9h-1"
    const val TRASH = "M5,7h14 M10,7V4h4v3 M7,7l1,13h8l1,-13"
    const val INFO = "M12,3a9,9 0 1,0 0,18a9,9 0 1,0 0,-18 M12,11v6 M12,7.5L12.01,7.5"
    const val PENCIL = "M4,20l1,-4l11,-11l3,3l-11,11z"
    const val ARCHIVE = "M4,5h16v4h-16z M5,9v10h14v-10 M10,13h4"
    const val UNZIP = "M4,5h16v4h-16z M5,9v10h14v-10 M12,17v-6 M9,14l3,-3l3,3"
    const val DUP = "M9,9h11v11h-11z M4,15V4h11 M14.5,12v5 M12,14.5h5"
    const val EYE = "M2,12c3,-5 7,-7 10,-7s7,2 10,7c-3,5 -7,7 -10,7s-7,-2 -10,-7z M12,9a3,3 0 1,0 0.01,0"
    const val TAG = "M3,12V4h8l10,10l-8,8z M7,8L7.01,8"
    const val DOWNLOAD = "M12,4v11 M7,11l5,5l5,-5 M5,20h14"
    const val CLOCK = "M12,3a9,9 0 1,0 0,18a9,9 0 1,0 0,-18 M12,7v5l3,2"
    const val PEOPLE = "M9,8a3,3 0 1,0 0.01,0 M3,20c0,-4 3,-6 6,-6s6,2 6,6 M17,8a2.5,2.5 0 1,0 0.01,0 M17,14c3,0 4,2 4,5"
    const val PHONE = "M8,3h8a1.5,1.5 0 0,1 1.5,1.5v15a1.5,1.5 0 0,1 -1.5,1.5h-8a1.5,1.5 0 0,1 -1.5,-1.5v-15a1.5,1.5 0 0,1 1.5,-1.5z M11,18h2"
    const val CHECK = "M5,12.5L10,17.5L19,7"
    const val GRID = "M4,4h6v6h-6z M14,4h6v6h-6z M4,14h6v6h-6z M14,14h6v6h-6z"
    const val LIST = "M4,6h16 M4,12h16 M4,18h16"
    const val PLAY = "M7,4L20,12L7,20z"
    const val PAUSE = "M6,4h4v16h-4z M14,4h4v16h-4z"
    const val NOTE = "M9,18V5l12,-2v13 M9,18a3,3 0 1,1 -6,0a3,3 0 0,1 6,0 M21,16a3,3 0 1,1 -6,0a3,3 0 0,1 6,0"
}

class IconDrawable(private val pathData: String, col: Int, private val stroke: Boolean = true, sw: Float = 1.8f) : Drawable() {
    private val path = PathParser.createPathFromPathData(pathData)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = if (pathData == P.MORE) 3.4f else sw
        color = col
    }

    override fun draw(c: Canvas) {
        val b = bounds
        val s = b.width() / 24f
        c.save()
        c.translate(b.left.toFloat(), b.top.toFloat())
        c.scale(s, s)
        c.drawPath(path, paint)
        c.restore()
    }

    override fun setAlpha(a: Int) { paint.alpha = a }
    override fun setColorFilter(f: ColorFilter?) { paint.colorFilter = f }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

fun Context.icon(path: String, col: Int, sizeDp: Int, stroke: Boolean = true, sw: Float = 1.8f): ImageView =
    ImageView(this).apply {
        setImageDrawable(IconDrawable(path, col, stroke, sw))
        layoutParams = ViewGroup.LayoutParams(dpi(sizeDp), dpi(sizeDp))
    }

/** Folder biru ala iOS. */
class FolderDrawable : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        val r = w * 0.1f
        c.save()
        c.translate(b.left.toFloat(), b.top.toFloat())
        p.shader = null
        p.color = 0xFF3FA5E8.toInt()
        c.drawRoundRect(RectF(0f, 0f, w * 0.42f, h * 0.4f), r, r, p)
        c.drawRoundRect(RectF(0f, h * 0.12f, w, h), r, r, p)
        p.shader = LinearGradient(0f, h * 0.22f, 0f, h, 0xFF8AD3FA.toInt(), 0xFF5BBBF2.toInt(), Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(0f, h * 0.24f, w, h), r, r, p)
        p.shader = null
        c.restore()
    }

    override fun setAlpha(a: Int) {}
    override fun setColorFilter(f: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

object Thumbs {
    lateinit var ctx: Context
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val pool = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    fun load(f: File, px: Int, cb: (Bitmap?) -> Unit) {
        val e = f.extension.lowercase()
        val img = e in FileOps.IMG
        val vid = e in FileOps.VID
        val apk = e == "apk"
        if (!img && !vid && !apk) return
        val key = f.absolutePath + f.lastModified()
        val hit = cache.get(key)
        if (hit != null) { cb(hit); return }
        pool.execute {
            val b: Bitmap? = try {
                when {
                    img -> decode(f, px)
                    vid -> video(f, px)
                    else -> apkIcon(f, px)
                }
            } catch (t: Throwable) { null }
            if (b != null) cache.put(key, b)
            main.post { cb(b) }
        }
    }

    fun decode(f: File, px: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, o)
        var s = 1
        while (minOf(o.outWidth, o.outHeight) / (s * 2) >= px) s *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = s }
        return BitmapFactory.decodeFile(f.absolutePath, o2)
    }

    private fun video(f: File, px: Int): Bitmap? {
        val m = MediaMetadataRetriever()
        try {
            m.setDataSource(f.absolutePath)
            val b = m.getFrameAtTime(0) ?: return null
            val sc = px.toFloat() / minOf(b.width, b.height)
            return if (sc < 1f) Bitmap.createScaledBitmap(b, (b.width * sc).toInt(), (b.height * sc).toInt(), true) else b
        } finally {
            try { m.release() } catch (_: Throwable) {}
        }
    }

    private fun apkIcon(f: File, px: Int): Bitmap? {
        val pm = ctx.packageManager
        val pi = pm.getPackageArchiveInfo(f.absolutePath, 0) ?: return null
        val ai = pi.applicationInfo ?: return null
        ai.sourceDir = f.absolutePath
        ai.publicSourceDir = f.absolutePath
        val d = ai.loadIcon(pm)
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        d.setBounds(0, 0, px, px)
        d.draw(c)
        return b
    }
}

/** Thumbnail: folder iOS, foto/video, atau halaman dokumen dengan label ekstensi. */
class FileThumbView(ctx: Context) : View(ctx) {
    var file: File? = null
    var bmp: Bitmap? = null
    private val folder = FolderDrawable()
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    fun bind(f: File) {
        file = f
        bmp = null
        invalidate()
        if (f.isDirectory) return
        Thumbs.load(f, 256) { b -> if (file == f) { bmp = b; invalidate() } }
    }

    override fun onDraw(canvas: Canvas) {
        val f = file ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        if (f.isDirectory) {
            folder.setBounds(0, (h * 0.1f).toInt(), width, (h * 0.92f).toInt())
            folder.draw(canvas)
            return
        }
        val b = bmp
        if (b != null) {
            val r = RectF(w * 0.04f, h * 0.04f, w * 0.96f, h * 0.96f)
            canvas.save()
            canvas.clipPath(Path().apply { addRoundRect(r, w * 0.08f, w * 0.08f, Path.Direction.CW) })
            val s = maxOf(r.width() / b.width, r.height() / b.height)
            val dw = b.width * s
            val dh = b.height * s
            canvas.drawBitmap(b, null, RectF(r.centerX() - dw / 2, r.centerY() - dh / 2, r.centerX() + dw / 2, r.centerY() + dh / 2), p)
            canvas.restore()
            return
        }
        val page = RectF(w * 0.2f, h * 0.04f, w * 0.8f, h * 0.96f)
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        canvas.drawRoundRect(page, w * 0.06f, w * 0.06f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        p.color = 0xFFD1D1D6.toInt()
        canvas.drawRoundRect(page, w * 0.06f, w * 0.06f, p)
        val e = f.extension.lowercase().take(4).uppercase()
        val col = when {
            f.extension.lowercase() in FileOps.AUD -> 0xFFFF2D55.toInt()
            f.extension.lowercase() in FileOps.VID -> 0xFF5856D6.toInt()
            f.extension.lowercase() in FileOps.TXT -> 0xFF8E8E93.toInt()
            f.extension.lowercase() == "apk" -> 0xFF34C759.toInt()
            f.extension.lowercase() == "zip" -> 0xFFFF9500.toInt()
            else -> C.BLUE
        }
        tp.color = col
        tp.textSize = w * (if (e.length > 3) 0.15f else 0.19f)
        canvas.drawText(e, w / 2, h / 2 + tp.textSize / 3, tp)
    }
}

/**
 * EditText dengan kursor biru iOS yang bergerak halus (animasi geser) dan berkedip.
 * Kursor bawaan Android disembunyikan, kursor ini digambar sendiri.
 */
class IosEditText(ctx: Context) : EditText(ctx) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C.BLUE }
    private var cx = 0f
    private var cy = 0f
    private var ch = 0f
    private var tx = Float.NaN
    private var ty = Float.NaN
    private var inited = false
    private var blink = true
    private var anim: ValueAnimator? = null
    private val blinker = object : Runnable {
        override fun run() {
            blink = !blink
            invalidate()
            postDelayed(this, 530)
        }
    }

    init {
        isCursorVisible = false
        background = null
        highlightColor = 0x55007AFF
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        removeCallbacks(blinker)
        inited = false
        blink = true
        if (focused) postDelayed(blinker, 530)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(blinker)
        anim?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isFocused || selectionStart != selectionEnd || selectionStart < 0) return
        val l = layout ?: return
        val o = selectionEnd
        val line = l.getLineForOffset(o)
        val nx = l.getPrimaryHorizontal(o) + compoundPaddingLeft
        val top = (l.getLineTop(line) + extendedPaddingTop).toFloat()
        val nh = (l.getLineBottom(line) - l.getLineTop(line)).toFloat()
        if (!inited) {
            cx = nx; cy = top; ch = nh; tx = nx; ty = top; inited = true
        } else if (nx != tx || top != ty) {
            tx = nx; ty = top; ch = nh
            anim?.cancel()
            val sx = cx
            val sy = cy
            anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 150
                interpolator = IOS_EASE
                addUpdateListener {
                    val f = it.animatedFraction
                    cx = sx + (tx - sx) * f
                    cy = sy + (ty - sy) * f
                    invalidate()
                }
                start()
            }
        }
        val running = anim?.isRunning == true
        if (blink || running) {
            val w = resources.displayMetrics.density * 2.2f
            canvas.drawRoundRect(cx - w / 2, cy, cx + w / 2, cy + ch, w / 2, w / 2, paint)
        }
    }
}
