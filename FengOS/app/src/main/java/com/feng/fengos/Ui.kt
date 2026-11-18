package com.feng.fengos

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object U {
    var tf: Typeface = Typeface.DEFAULT
    var tfBold: Typeface = Typeface.DEFAULT_BOLD
    const val BLUE = 0xFF0A84FF.toInt()

    /** Taruh font SF Pro milikmu di app/src/main/assets/fonts/sf_pro.ttf (opsional). */
    fun init(c: Context) {
        try {
            tf = Typeface.createFromAsset(c.assets, "fonts/sf_pro.ttf")
            tfBold = Typeface.create(tf, Typeface.BOLD)
        } catch (e: Exception) {
            tf = Typeface.create("sans-serif", Typeface.NORMAL)
            tfBold = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }
}

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpi(v: Float): Int = (v * resources.displayMetrics.density).toInt()

/** Putih dengan alpha 0..255 */
fun cw(a: Int): Int = Color.argb(a, 255, 255, 255)

/** Hitam dengan alpha 0..255 */
fun cb(a: Int): Int = Color.argb(a, 0, 0, 0)

fun Context.label(
    s: String,
    sp: Float,
    color: Int = Color.WHITE,
    bold: Boolean = false,
    gravity: Int = Gravity.START
): TextView {
    val t = TextView(this)
    t.text = s
    t.textSize = sp
    t.setTextColor(color)
    t.typeface = if (bold) U.tfBold else U.tf
    t.gravity = gravity
    t.includeFontPadding = false
    return t
}

/** Kotak kaca (semi transparan + garis tepi tipis) */
fun Context.glass(radius: Float, fill: Int = cw(64), stroke: Int = cw(90)): GradientDrawable {
    val g = GradientDrawable()
    g.shape = GradientDrawable.RECTANGLE
    g.cornerRadius = dp(radius)
    g.setColor(fill)
    g.setStroke(maxOf(1, dpi(1f)), stroke)
    return g
}

/** Superellipse (n=5) — bentuk "squircle" ikon iOS */
fun squirclePath(size: Float, n: Double = 5.0): Path {
    val p = Path()
    val r = size / 2f
    val steps = 160
    for (i in 0..steps) {
        val t = 2.0 * Math.PI * i / steps
        val ct = Math.cos(t)
        val st = Math.sin(t)
        val x = r + r * (sign(ct) * abs(ct).pow(2.0 / n)).toFloat()
        val y = r + r * (sign(st) * abs(st).pow(2.0 / n)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

class SquircleIcon(c: Context, d: Drawable?, private val s: Int) : View(c) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = squirclePath(s.toFloat())

    init {
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        if (d != null) drawIcon(d, cv)
        paint.shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    private fun drawIcon(d: Drawable, cv: Canvas) {
        if (d is AdaptiveIconDrawable) {
            // gambar layer penuh tanpa mask bawaan, lalu dipotong squircle
            val l = (s * 0.25f).toInt()
            val bg = d.background
            val fg = d.foreground
            if (bg != null) {
                bg.setBounds(-l, -l, s + l, s + l)
                bg.draw(cv)
            }
            if (fg != null) {
                fg.setBounds(-l, -l, s + l, s + l)
                fg.draw(cv)
            }
        } else {
            cv.drawColor(Color.rgb(242, 242, 247))
            val pad = (s * 0.12f).toInt()
            d.setBounds(pad, pad, s - pad, s - pad)
            d.draw(cv)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(s, s)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }
}

data class AppInfo(
    val label: String,
    val pkg: String,
    val intent: Intent,
    val icon: Drawable,
    val cat: String
)

fun catName(cat: Int): String = when (cat) {
    ApplicationInfo.CATEGORY_GAME -> "Games"
    ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> "Entertainment"
    ApplicationInfo.CATEGORY_IMAGE -> "Creativity"
    ApplicationInfo.CATEGORY_SOCIAL -> "Social"
    ApplicationInfo.CATEGORY_NEWS -> "Reading & Info"
    ApplicationInfo.CATEGORY_MAPS -> "Travel"
    ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
    else -> "Utilities"
}

fun loadApps(c: Context): List<AppInfo> {
    val pm = c.packageManager
    val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(q, 0)
        .filter { it.activityInfo.packageName != c.packageName }
        .map { ri ->
            val pkg = ri.activityInfo.packageName
            val launch = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(pkg, ri.activityInfo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            AppInfo(
                ri.loadLabel(pm).toString(),
                pkg,
                launch,
                ri.loadIcon(pm),
                catName(ri.activityInfo.applicationInfo.category)
            )
        }
        .sortedBy { it.label.lowercase() }
}
