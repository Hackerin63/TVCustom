package com.arun.aurora

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import java.io.File
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// Lightweight animated wallpaper (30 fps, plain Canvas, no textures) for a 1 GB TV.
class LiveWallpaperView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    var animated = true
    private var th = Theme.AURORA
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rnd = Random()
    private var photo: Bitmap? = null
    private var bg: Shader? = null
    private var last = 0L
    private var tm = 0f
    private val dens = resources.displayMetrics.density

    private val snowN = 70
    private val sx = FloatArray(snowN); private val sy = FloatArray(snowN)
    private val sr = FloatArray(snowN); private val sv = FloatArray(snowN); private val sd = FloatArray(snowN)

    private val fireN = 90
    private val fx = FloatArray(fireN); private val fy = FloatArray(fireN)
    private val flife = FloatArray(fireN); private val fmax = FloatArray(fireN)
    private val fsize = FloatArray(fireN); private val fvx = FloatArray(fireN)

    private val carN = 6
    private val cx = FloatArray(carN); private val cl = IntArray(carN)
    private val cc = IntArray(carN)
    private val laneSpeed = floatArrayOf(160f, 260f, 380f)
    private val carColors = intArrayOf(0xFFFF4D4D.toInt(), 0xFF42A5F5.toInt(), 0xFFFFCA28.toInt(),
        0xFF66BB6A.toInt(), 0xFFAB47BC.toInt(), 0xFFFFFFFF.toInt())
    private var roadOff = 0f
    private val stX = FloatArray(14); private val stY = FloatArray(14); private val stV = FloatArray(14)

    private var blobs: Array<Shader> = emptyArray()
    private var blobR = 0f

    fun setTheme(t: Theme) {
        th = t
        if (t == Theme.PHOTO) loadPhoto()
        buildBackground()
        initState()
        invalidate()
    }

    private fun loadPhoto() {
        val f = File(context.getExternalFilesDir(null), "wallpaper.jpg")
        photo = if (f.exists()) {
            val o = BitmapFactory.Options().apply { inSampleSize = 2; inPreferredConfig = Bitmap.Config.RGB_565 }
            BitmapFactory.decodeFile(f.path, o)
        } else null
    }

    private fun buildBackground() {
        val h = height.toFloat()
        if (h == 0f) return
        val (c1, c2) = when (th) {
            Theme.AURORA, Theme.PHOTO -> 0xFF0B0F1A.toInt() to 0xFF1B2440.toInt()
            Theme.SNOW -> 0xFF0A1830.toInt() to 0xFF24508C.toInt()
            Theme.CARS -> 0xFF05060F.toInt() to 0xFF3A1A4A.toInt()
            Theme.FIRE -> 0xFF050100.toInt() to 0xFF3A0D00.toInt()
        }
        bg = LinearGradient(0f, 0f, 0f, h, c1, c2, Shader.TileMode.CLAMP)
        blobR = h * 0.8f
        blobs = arrayOf(
            RadialGradient(0f, 0f, blobR, 0x806EA8FF.toInt(), 0x006EA8FF, Shader.TileMode.CLAMP),
            RadialGradient(0f, 0f, blobR, 0x808A5CFF.toInt(), 0x008A5CFF, Shader.TileMode.CLAMP),
            RadialGradient(0f, 0f, blobR, 0x8040E0D0.toInt(), 0x0040E0D0, Shader.TileMode.CLAMP)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        buildBackground()
        initState()
    }

    private fun initState() {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f) return
        for (i in 0 until snowN) {
            sx[i] = rnd.nextFloat() * w; sy[i] = rnd.nextFloat() * h
            sr[i] = (1.2f + rnd.nextFloat() * 3.2f) * dens
            sv[i] = 30f * dens + sr[i] * 10f
            sd[i] = (8f + rnd.nextFloat() * 25f) * dens
        }
        for (i in 0 until fireN) {
            respawnFire(i, w, h)
            flife[i] = rnd.nextFloat() * fmax[i]
        }
        for (i in 0 until carN) {
            cl[i] = i % 3
            cx[i] = if (i < 3) w * 0.1f else w * 0.6f
            cc[i] = carColors[i % carColors.size]
        }
        for (i in stX.indices) {
            stX[i] = rnd.nextFloat() * w; stY[i] = h * (0.28f + rnd.nextFloat() * 0.33f)
            stV[i] = 700f + rnd.nextFloat() * 700f
        }
    }

    private fun respawnFire(i: Int, w: Float, h: Float) {
        fmax[i] = 0.9f + rnd.nextFloat() * 1.0f
        flife[i] = fmax[i]
        fx[i] = w * (0.05f + 0.9f * rnd.nextFloat())
        fy[i] = h + 10f
        fsize[i] = (14f + rnd.nextFloat() * 22f) * dens
        fvx[i] = (rnd.nextFloat() - 0.5f) * 40f * dens
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f) return
        val now = System.nanoTime()
        val dt = if (!animated) 0f else if (last == 0L) 0.033f else ((now - last) / 1e9f).coerceIn(0f, 0.1f)
        last = now; tm += dt

        p.shader = bg
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        when (th) {
            Theme.AURORA -> drawAurora(c, w, h)
            Theme.SNOW -> drawSnow(c, w, h, dt)
            Theme.CARS -> drawCars(c, w, h, dt)
            Theme.FIRE -> drawFire(c, w, h, dt)
            Theme.PHOTO -> drawPhoto(c, w, h)
        }
        if (animated) postInvalidateDelayed(33)
    }

    fun setAnimatedFlag(v: Boolean) { animated = v; last = 0L; invalidate() }

    private fun drawAurora(c: Canvas, w: Float, h: Float) {
        if (blobs.isEmpty()) return
        for (i in 0 until 3) {
            val x = w * (0.5f + 0.35f * sin(tm * 0.2f + i * 2.1f))
            val y = h * (0.45f + 0.25f * cos(tm * 0.17f + i * 1.7f))
            c.save(); c.translate(x, y)
            p.shader = blobs[i]
            c.drawCircle(0f, 0f, blobR, p)
            c.restore()
        }
        p.shader = null
    }

    private fun drawPhoto(c: Canvas, w: Float, h: Float) {
        val b = photo
        if (b == null) { drawAurora(c, w, h); return }
        val sc = max(w / b.width, h / b.height)
        val dw = b.width * sc; val dh = b.height * sc
        c.drawBitmap(b, null, RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f), null)
        p.color = 0x55000000
        c.drawRect(0f, 0f, w, h, p)
    }

    private fun drawSnow(c: Canvas, w: Float, h: Float, dt: Float) {
        for (i in 0 until snowN) {
            sy[i] += sv[i] * dt
            sx[i] += sin(tm * 0.8f + i) * sd[i] * dt
            if (sy[i] > h + 10f) { sy[i] = -10f; sx[i] = rnd.nextFloat() * w }
            if (sx[i] > w + 10f) sx[i] = -10f else if (sx[i] < -10f) sx[i] = w + 10f
            p.color = Color.argb(minOf(255, (110 + sr[i] * 25f).toInt()), 255, 255, 255)
            c.drawCircle(sx[i], sy[i], sr[i], p)
        }
    }

    private fun drawFire(c: Canvas, w: Float, h: Float, dt: Float) {
        for (i in 0 until fireN) {
            flife[i] -= dt
            if (flife[i] <= 0f) respawnFire(i, w, h)
            val f = flife[i] / fmax[i]
            fy[i] -= (80f + 220f * f) * dens * dt
            fx[i] += (fvx[i] + sin(tm * 3f + i) * 25f) * dt
            val col = when {
                f > 0.66f -> 0xFFFFE08A.toInt()
                f > 0.33f -> 0xFFFF8A3D.toInt()
                else -> 0xFFC21500.toInt()
            }
            val a = (f * 200f).toInt().coerceIn(0, 255)
            p.color = (col and 0x00FFFFFF) or (a shl 24)
            c.drawCircle(fx[i], fy[i], fsize[i] * (0.35f + f), p)
        }
    }

    private fun drawCars(c: Canvas, w: Float, h: Float, dt: Float) {
        val roadTop = h * 0.26f; val roadBot = h * 0.62f
        val laneH = (roadBot - roadTop) / 3f
        p.color = 0xFF14161F.toInt()
        c.drawRect(0f, roadTop, w, roadBot, p)
        p.color = 0xFFCCCCCC.toInt()
        c.drawRect(0f, roadTop, w, roadTop + 4f, p)
        c.drawRect(0f, roadBot - 4f, w, roadBot, p)

        roadOff = (roadOff + 260f * dens * dt) % 80f
        p.color = 0x99FFFFFF.toInt()
        for (l in 1..2) {
            val y = roadTop + laneH * l
            var x = -roadOff
            while (x < w) { c.drawRect(x, y - 2f, x + 40f, y + 2f, p); x += 80f }
        }

        p.color = 0x44FFFFFF
        for (i in stX.indices) {
            stX[i] -= stV[i] * dt
            if (stX[i] < -200f) { stX[i] = w + rnd.nextFloat() * 200f; stY[i] = roadTop + rnd.nextFloat() * (roadBot - roadTop) }
            c.drawRect(stX[i], stY[i], stX[i] + 90f, stY[i] + 2f, p)
        }

        val carW = h * 0.2f; val carH = h * 0.05f
        for (i in 0 until carN) {
            cx[i] += laneSpeed[cl[i]] * dens * dt
            if (cx[i] > w + carW) cx[i] = -carW * 1.5f
            val y = roadTop + laneH * (cl[i] + 0.5f) + carH * 0.8f
            drawCar(c, cx[i], y, carW, carH, cc[i])
        }
    }

    private fun drawCar(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        p.color = 0x55FFF3B0
        c.drawCircle(x + w, y - h * 0.5f, h * 1.1f, p)
        p.color = color
        c.drawRoundRect(x, y - h, x + w, y, h * 0.35f, h * 0.35f, p)
        p.color = 0xFF1A2233.toInt()
        c.drawRoundRect(x + w * 0.25f, y - h * 1.65f, x + w * 0.72f, y - h * 0.9f, h * 0.25f, h * 0.25f, p)
        p.color = 0xFF0A0A0A.toInt()
        c.drawCircle(x + w * 0.22f, y, h * 0.3f, p)
        c.drawCircle(x + w * 0.78f, y, h * 0.3f, p)
    }
}
