package com.arun.aurora

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.widget.TextView

object Ui {
    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun tileBg(ctx: Context, accent: Int, focused: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(ctx, 18).toFloat()
        setColor(if (focused) 0x40FFFFFF else 0x24FFFFFF)
        if (focused) setStroke(dp(ctx, 3), accent)
    }

    fun button(ctx: Context, label: String, accent: Int, onClick: () -> Unit): TextView = TextView(ctx).apply {
        text = label
        setTextColor(Color.WHITE)
        textSize = 15f
        gravity = Gravity.CENTER
        setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10))
        isFocusable = true
        isClickable = true
        background = tileBg(ctx, accent, false)
        setOnClickListener { onClick() }
        setOnFocusChangeListener { v, f ->
            v.background = tileBg(ctx, accent, f)
            val s = if (f) 1.06f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(120).start()
        }
    }

    fun cardBg(ctx: Context, color: Int = 0x40101626, radius: Int = 22): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(ctx, radius).toFloat()
        setColor(color)
        setStroke(dp(ctx, 1), 0x22FFFFFF)
    }

    fun openSettings(ctx: Context, action: String) {
        try {
            ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            try { ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            catch (_: Exception) { }
        }
    }
}

// Colored badge with a big letter, used for built-in tiles (games, themes)
class BadgeDrawable(private val letter: String, private val c1: Int, private val c2: Int) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas: Canvas) {
        val b = bounds
        p.shader = LinearGradient(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), c1, c2, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(b), 18f, 18f, p)
        p.shader = null
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.textSize = b.height() * 0.55f
        p.isFakeBoldText = true
        canvas.drawText(letter, b.exactCenterX(), b.exactCenterY() + p.textSize * 0.35f, p)
    }
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
