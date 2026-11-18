package com.feng.fengos

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Layar boot: logo putih di latar hitam + bar progres tipis. */
class BootView(c: Context, private val onDone: () -> Unit) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var prog = 0f
    private var logoAlpha = 0f

    init {
        setBackgroundColor(Color.BLACK)
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = 3200
        anim.addUpdateListener {
            prog = it.animatedValue as Float
            logoAlpha = (prog * 4f).coerceAtMost(1f)
            invalidate()
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onDone()
            }
        })
        anim.startDelay = 400
        anim.start()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        p.style = Paint.Style.FILL
        p.typeface = U.tfBold
        p.textAlign = Paint.Align.CENTER
        p.textSize = w * 0.26f
        p.color = Color.WHITE
        p.alpha = (logoAlpha * 255).toInt()
        canvas.drawText("F", w / 2f, h / 2f + p.textSize * 0.35f - h * 0.03f, p)

        val bw = w * 0.30f
        val bh = context.dp(3f)
        val by = h * 0.60f
        val left = w / 2f - bw / 2f
        p.color = Color.argb((60 * logoAlpha).toInt(), 255, 255, 255)
        canvas.drawRoundRect(RectF(left, by, left + bw, by + bh), bh, bh, p)
        p.color = Color.argb((255 * logoAlpha).toInt(), 255, 255, 255)
        canvas.drawRoundRect(RectF(left, by, left + bw * prog, by + bh), bh, bh, p)
    }
}
