package com.arun.aurora

import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

enum class Glyph { WIFI, BLUETOOTH, GEAR, GLOBE }

// Small round status/shortcut button drawn with Canvas (no image assets)
class RoundIconView(ctx: Context, private val glyph: Glyph) : View(ctx) {
    var accent: Int = 0xFF6EA8FF.toInt()
    var active: Boolean = true
        set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isFocusable = true
        isClickable = true
        setOnFocusChangeListener { v, f ->
            val s = if (f) 1.15f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(160).setInterpolator(DecelerateInterpolator()).start()
            invalidate()
        }
    }

    override fun onMeasure(w: Int, h: Int) {
        val s = Ui.dp(context, 40)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val s = width.toFloat(); val cx = s / 2f; val cy = s / 2f
        p.style = Paint.Style.FILL
        p.color = if (hasFocus()) 0x55FFFFFF else 0x30FFFFFF
        c.drawCircle(cx, cy, s / 2f, p)
        if (hasFocus()) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = Ui.dp(context, 2).toFloat()
            p.color = accent
            c.drawCircle(cx, cy, s / 2f - Ui.dp(context, 1), p)
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = s * 0.065f
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.color = if (active) Color.WHITE else 0x66FFFFFF

        when (glyph) {
            Glyph.WIFI -> {
                val ay = cy + s * 0.16f
                for (r in floatArrayOf(s * 0.12f, s * 0.22f, s * 0.32f))
                    c.drawArc(RectF(cx - r, ay - r, cx + r, ay + r), -135f, 90f, false, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, ay, s * 0.035f, p)
            }
            Glyph.BLUETOOTH -> {
                val pts = floatArrayOf(0.3f, 0.3f, 0.7f, 0.7f, 0.5f, 0.85f, 0.5f, 0.15f, 0.7f, 0.3f, 0.3f, 0.7f)
                val path = Path()
                var i = 0
                while (i < pts.size) {
                    val x = cx + (pts[i] - 0.5f) * s * 0.7f
                    val y = cy + (pts[i + 1] - 0.5f) * s * 0.7f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    i += 2
                }
                c.drawPath(path, p)
            }
            Glyph.GEAR -> {
                c.drawCircle(cx, cy, s * 0.14f, p)
                for (k in 0 until 8) {
                    val a = Math.toRadians(k * 45.0)
                    val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
                    c.drawLine(cx + ca * s * 0.21f, cy + sa * s * 0.21f, cx + ca * s * 0.3f, cy + sa * s * 0.3f, p)
                }
            }
            Glyph.GLOBE -> {
                c.drawCircle(cx, cy, s * 0.28f, p)
                c.drawOval(RectF(cx - s * 0.12f, cy - s * 0.28f, cx + s * 0.12f, cy + s * 0.28f), p)
                c.drawLine(cx - s * 0.28f, cy, cx + s * 0.28f, cy, p)
            }
        }
    }
}

// Rounded, translucent pop-up menu (replaces the rigid system dialog)
object RoundDialog {
    fun show(ctx: Context, title: String, labels: List<String>, accent: Int, onPick: (Int) -> Unit) {
        val d = Dialog(ctx)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = Ui.dp(ctx, 22)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                cornerRadius = Ui.dp(ctx, 28).toFloat()
                setColor(0xE8141B2D.toInt())
                setStroke(Ui.dp(ctx, 1), 0x33FFFFFF)
            }
        }
        box.addView(TextView(ctx).apply {
            text = title
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(Ui.dp(ctx, 4), 0, 0, Ui.dp(ctx, 6))
        })
        labels.forEachIndexed { i, l ->
            val b = Ui.button(ctx, l, accent) { d.dismiss(); onPick(i) }
            box.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = Ui.dp(ctx, 8) })
        }
        d.setContentView(box)
        d.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(Ui.dp(ctx, 380), ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.55f)
        }
        box.alpha = 0f; box.scaleX = 0.92f; box.scaleY = 0.92f
        d.show()
        box.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
        box.getChildAt(1)?.requestFocus()
    }
}
