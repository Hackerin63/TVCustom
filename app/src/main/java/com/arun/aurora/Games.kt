package com.arun.aurora

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import java.util.Random

abstract class GameView(ctx: Context) : View(ctx) {
    private val ui = Handler(Looper.getMainLooper())
    private var running = false
    protected var waiting = true
    protected val p = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val rnd = Random()
    protected abstract val gameName: String

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            if (!waiting) step()
            invalidate()
            ui.postDelayed(this, nextDelay())
        }
    }

    protected abstract fun step()
    protected abstract fun nextDelay(): Long
    abstract fun onKey(code: Int): Boolean

    fun handleKey(code: Int): Boolean {
        if (waiting) {
            if (isOk(code)) { waiting = false; invalidate(); return true }
            return false
        }
        return onKey(code)
    }

    fun resume() { if (!running) { running = true; ui.postDelayed(loop, nextDelay()) } }
    fun pause() { running = false; ui.removeCallbacks(loop) }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); resume() }
    override fun onDetachedFromWindow() { pause(); super.onDetachedFromWindow() }

    protected fun isOk(code: Int) = code == KeyEvent.KEYCODE_DPAD_CENTER ||
        code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_BUTTON_A

    protected fun label(c: Canvas, s: String, x: Float, y: Float, size: Float,
                        color: Int = Color.WHITE, align: Paint.Align = Paint.Align.LEFT) {
        p.shader = null; p.style = Paint.Style.FILL
        p.color = color; p.textSize = size; p.textAlign = align; p.isFakeBoldText = true
        c.drawText(s, x, y, p)
        p.isFakeBoldText = false
    }

    // Rounded pop-up used for start / game-over
    protected fun panel(c: Canvas, title: String, sub: String) {
        val w = width.toFloat(); val h = height.toFloat()
        val pw = w * 0.42f; val ph = h * 0.32f
        val r = RectF((w - pw) / 2f, (h - ph) / 2f, (w + pw) / 2f, (h + ph) / 2f)
        p.shader = null; p.style = Paint.Style.FILL; p.color = 0xE8141B2D.toInt()
        c.drawRoundRect(r, 40f, 40f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = 0x44FFFFFF
        c.drawRoundRect(r, 40f, 40f, p)
        p.style = Paint.Style.FILL
        label(c, title, w / 2f, r.top + ph * 0.48f, ph * 0.22f, Color.WHITE, Paint.Align.CENTER)
        label(c, sub, w / 2f, r.top + ph * 0.76f, ph * 0.11f, 0xFFB8C2DD.toInt(), Paint.Align.CENTER)
    }

    protected fun hud(c: Canvas, s: String) {
        val w = p.also { it.textSize = 26f }.measureText(s)
        val r = RectF(20f, 16f, 20f + w + 40f, 66f)
        p.style = Paint.Style.FILL; p.color = 0x66000000
        c.drawRoundRect(r, 25f, 25f, p)
        label(c, s, 40f, 51f, 26f)
    }
}

class SnakeView(ctx: Context) : GameView(ctx) {
    override val gameName = "Snake"
    private val cols = 32
    private val rows = 18
    private val sx = ArrayList<Int>()
    private val sy = ArrayList<Int>()
    private var dx = 1; private var dy = 0; private var ndx = 1; private var ndy = 0
    private var fx = 0; private var fy = 0
    private var score = 0; private var best = 0; private var dead = false

    init { reset() }

    private fun reset() {
        sx.clear(); sy.clear()
        for (i in 0 until 3) { sx.add(8 - i); sy.add(9) }
        dx = 1; dy = 0; ndx = 1; ndy = 0
        score = 0; dead = false
        placeFood()
    }

    private fun onSnake(x: Int, y: Int): Boolean {
        for (i in sx.indices) if (sx[i] == x && sy[i] == y) return true
        return false
    }

    private fun placeFood() {
        do { fx = rnd.nextInt(cols); fy = rnd.nextInt(rows) } while (onSnake(fx, fy))
    }

    override fun nextDelay(): Long = maxOf(55L, 130L - score * 3L)

    override fun step() {
        if (dead) return
        dx = ndx; dy = ndy
        val nx = (sx[0] + dx + cols) % cols
        val ny = (sy[0] + dy + rows) % rows
        val eating = nx == fx && ny == fy
        val limit = if (eating) sx.size else sx.size - 1
        for (i in 0 until limit) {
            if (sx[i] == nx && sy[i] == ny) { dead = true; if (score > best) best = score; return }
        }
        sx.add(0, nx); sy.add(0, ny)
        if (eating) { score++; placeFood() } else { sx.removeAt(sx.size - 1); sy.removeAt(sy.size - 1) }
    }

    override fun onKey(code: Int): Boolean {
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> if (dy == 0) { ndx = 0; ndy = -1 }
            KeyEvent.KEYCODE_DPAD_DOWN -> if (dy == 0) { ndx = 0; ndy = 1 }
            KeyEvent.KEYCODE_DPAD_LEFT -> if (dx == 0) { ndx = -1; ndy = 0 }
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (dx == 0) { ndx = 1; ndy = 0 }
            else -> { if (isOk(code)) { if (dead) reset() } else return false }
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(0xFF08140E.toInt())
        val cell = minOf(width / cols.toFloat(), height / rows.toFloat())
        val bw = cell * cols; val bh = cell * rows
        val ox = (width - bw) / 2f; val oy = (height - bh) / 2f
        p.style = Paint.Style.FILL
        p.color = 0xFF0E2217.toInt()
        c.drawRoundRect(RectF(ox, oy, ox + bw, oy + bh), 24f, 24f, p)
        p.color = 0xFFFF5252.toInt()
        c.drawCircle(ox + fx * cell + cell / 2f, oy + fy * cell + cell / 2f, cell * 0.38f, p)
        for (i in sx.indices) {
            p.color = if (i == 0) 0xFFB9F6CA.toInt() else 0xFF2ECC71.toInt()
            val l = ox + sx[i] * cell + 1.5f; val t = oy + sy[i] * cell + 1.5f
            c.drawRoundRect(RectF(l, t, l + cell - 3f, t + cell - 3f), cell * 0.35f, cell * 0.35f, p)
        }
        hud(c, "Score $score    Best $best")
        if (waiting) panel(c, gameName, "Press OK to start  -  Back to exit")
        else if (dead) panel(c, "Game Over", "Score $score  -  Press OK to retry")
    }
}

class BlocksView(ctx: Context) : GameView(ctx) {
    override val gameName = "Blocks"
    private val cols = 10
    private val rows = 20
    private val grid = Array(rows) { IntArray(cols) }
    private val shapes = arrayOf(
        intArrayOf(0, 1, 1, 1, 2, 1, 3, 1),
        intArrayOf(1, 0, 2, 0, 1, 1, 2, 1),
        intArrayOf(1, 0, 0, 1, 1, 1, 2, 1),
        intArrayOf(1, 0, 2, 0, 0, 1, 1, 1),
        intArrayOf(0, 0, 1, 0, 1, 1, 2, 1),
        intArrayOf(0, 0, 0, 1, 1, 1, 2, 1),
        intArrayOf(2, 0, 0, 1, 1, 1, 2, 1)
    )
    private val sizes = intArrayOf(4, 2, 3, 3, 3, 3, 3)
    private val colors = intArrayOf(0xFF00E5FF.toInt(), 0xFFFFD600.toInt(), 0xFFB388FF.toInt(), 0xFF69F0AE.toInt(),
        0xFFFF5252.toInt(), 0xFF448AFF.toInt(), 0xFFFFAB40.toInt())
    private var cur = IntArray(8)
    private var type = 0; private var nextType = 0
    private var px = 0; private var py = 0
    private var score = 0; private var lines = 0; private var over = false

    init { reset() }

    private fun reset() {
        for (r in grid) r.fill(0)
        score = 0; lines = 0; over = false
        nextType = rnd.nextInt(7)
        spawn()
    }

    private fun spawn() {
        type = nextType; nextType = rnd.nextInt(7)
        cur = shapes[type].copyOf(); px = 3; py = 0
        if (hit(cur, px, py)) over = true
    }

    private fun hit(cells: IntArray, ox: Int, oy: Int): Boolean {
        var i = 0
        while (i < 8) {
            val x = cells[i] + ox; val y = cells[i + 1] + oy
            if (x < 0 || x >= cols || y >= rows) return true
            if (y >= 0 && grid[y][x] != 0) return true
            i += 2
        }
        return false
    }

    private fun rotate() {
        if (type == 1) return
        val n = sizes[type]
        val r = IntArray(8)
        var i = 0
        while (i < 8) { r[i] = n - 1 - cur[i + 1]; r[i + 1] = cur[i]; i += 2 }
        for (k in intArrayOf(0, -1, 1, -2, 2)) {
            if (!hit(r, px + k, py)) { cur = r; px += k; return }
        }
    }

    private fun lock() {
        var i = 0
        while (i < 8) {
            val x = cur[i] + px; val y = cur[i + 1] + py
            if (y >= 0) grid[y][x] = type + 1
            i += 2
        }
        var cleared = 0
        var r = rows - 1
        while (r >= 0) {
            if (grid[r].all { it != 0 }) {
                for (k in r downTo 1) grid[k] = grid[k - 1].copyOf()
                grid[0] = IntArray(cols)
                cleared++
            } else r--
        }
        lines += cleared
        score += when (cleared) { 1 -> 100; 2 -> 300; 3 -> 500; 4 -> 800; else -> 0 }
        spawn()
    }

    override fun nextDelay(): Long = maxOf(100L, 600L - (lines / 10) * 50L)

    override fun step() {
        if (over) return
        if (!hit(cur, px, py + 1)) py++ else lock()
    }

    override fun onKey(code: Int): Boolean {
        if (over) {
            if (isOk(code)) { reset(); invalidate(); return true }
            return false
        }
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> if (!hit(cur, px - 1, py)) px--
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (!hit(cur, px + 1, py)) px++
            KeyEvent.KEYCODE_DPAD_DOWN -> if (!hit(cur, px, py + 1)) { py++; score++ }
            KeyEvent.KEYCODE_DPAD_UP -> rotate()
            else -> { if (isOk(code)) { while (!hit(cur, px, py + 1)) py++; lock() } else return false }
        }
        invalidate()
        return true
    }

    private fun block(c: Canvas, l: Float, t: Float, cell: Float, color: Int) {
        p.style = Paint.Style.FILL; p.color = color
        c.drawRoundRect(RectF(l + 1.5f, t + 1.5f, l + cell - 1.5f, t + cell - 1.5f), cell * 0.25f, cell * 0.25f, p)
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(0xFF0A0F1C.toInt())
        val cell = height * 0.92f / rows
        val bw = cell * cols; val bh = cell * rows
        val ox = (width - bw) / 2f - cell * 3f; val oy = (height - bh) / 2f
        p.style = Paint.Style.FILL; p.color = 0xFF121A2E.toInt()
        c.drawRoundRect(RectF(ox - 8f, oy - 8f, ox + bw + 8f, oy + bh + 8f), 28f, 28f, p)
        for (y in 0 until rows) for (x in 0 until cols) {
            val v = grid[y][x]
            if (v != 0) block(c, ox + x * cell, oy + y * cell, cell, colors[v - 1])
        }
        if (!over) {
            var i = 0
            while (i < 8) { block(c, ox + (cur[i] + px) * cell, oy + (cur[i + 1] + py) * cell, cell, colors[type]); i += 2 }
        }
        val sx0 = ox + bw + cell * 1.5f
        val side = RectF(sx0, oy, sx0 + cell * 6f, oy + cell * 9f)
        p.style = Paint.Style.FILL; p.color = 0x66000000
        c.drawRoundRect(side, 28f, 28f, p)
        label(c, "Score", sx0 + 24f, oy + cell * 1.2f, cell * 0.6f, 0xFFB8C2DD.toInt())
        label(c, "$score", sx0 + 24f, oy + cell * 2.2f, cell * 0.9f)
        label(c, "Lines $lines", sx0 + 24f, oy + cell * 3.4f, cell * 0.6f, 0xFFB8C2DD.toInt())
        label(c, "Next", sx0 + 24f, oy + cell * 4.6f, cell * 0.6f, 0xFFB8C2DD.toInt())
        val small = cell * 0.7f
        var j = 0
        while (j < 8) {
            block(c, sx0 + 30f + shapes[nextType][j] * small, oy + cell * 5.1f + shapes[nextType][j + 1] * small, small, colors[nextType])
            j += 2
        }
        if (waiting) panel(c, gameName, "D-pad move  -  Up rotate  -  OK drop")
        else if (over) panel(c, "Game Over", "Score $score  -  Press OK to retry")
    }
}

class CarView(ctx: Context) : GameView(ctx) {
    override val gameName = "Car Race"
    private var lane = 1
    private var pxLane = 1f
    private val obsLane = ArrayList<Int>()
    private val obsY = ArrayList<Float>()
    private val obsCol = ArrayList<Int>()
    private var dist = 0f
    private var speed = 320f
    private var spawnT = 0f
    private var dash = 0f
    private var dead = false
    private var best = 0
    private val dt = 1f / 30f
    private val cols = intArrayOf(0xFF42A5F5.toInt(), 0xFFFFCA28.toInt(), 0xFF66BB6A.toInt(), 0xFFAB47BC.toInt(), 0xFFFFFFFF.toInt())

    init { reset() }

    private fun reset() {
        lane = 1; pxLane = 1f
        obsLane.clear(); obsY.clear(); obsCol.clear()
        dist = 0f; speed = 320f; spawnT = 0.5f; dash = 0f; dead = false
    }

    private fun roadW() = height * 0.78f
    private fun carW() = roadW() / 3f * 0.55f
    private fun carH() = carW() * 1.9f
    private fun score() = (dist / 20f).toInt()
    override fun nextDelay(): Long = 33L

    override fun step() {
        if (dead || height == 0) return
        pxLane += (lane - pxLane) * 0.35f
        speed = 320f + dist / 40f
        dist += speed * dt
        dash = (dash + speed * dt) % 90f
        spawnT -= dt
        if (spawnT <= 0f) {
            obsLane.add(rnd.nextInt(3)); obsY.add(-carH() - 20f); obsCol.add(cols[rnd.nextInt(cols.size)])
            spawnT = maxOf(0.5f, 1.2f - dist / 6000f)
        }
        val top = height - carH() - 30f
        var i = 0
        while (i < obsY.size) {
            obsY[i] = obsY[i] + speed * 0.55f * dt
            if (obsY[i] > height + 50f) {
                obsLane.removeAt(i); obsY.removeAt(i); obsCol.removeAt(i)
                continue
            }
            if (Math.abs(pxLane - obsLane[i]) < 0.7f && obsY[i] + carH() > top + 10f && obsY[i] < top + carH() - 10f) {
                dead = true
                if (score() > best) best = score()
            }
            i++
        }
    }

    override fun onKey(code: Int): Boolean {
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> lane = maxOf(0, lane - 1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> lane = minOf(2, lane + 1)
            else -> { if (isOk(code)) { if (dead) reset() } else return false }
        }
        return true
    }

    private fun car(c: Canvas, cx: Float, top: Float, w: Float, h: Float, color: Int) {
        p.style = Paint.Style.FILL
        p.color = 0xFF0A0A0A.toInt()
        c.drawRoundRect(RectF(cx - w / 2f - 6f, top + h * 0.12f, cx - w / 2f + 4f, top + h * 0.34f), 4f, 4f, p)
        c.drawRoundRect(RectF(cx + w / 2f - 4f, top + h * 0.12f, cx + w / 2f + 6f, top + h * 0.34f), 4f, 4f, p)
        c.drawRoundRect(RectF(cx - w / 2f - 6f, top + h * 0.66f, cx - w / 2f + 4f, top + h * 0.88f), 4f, 4f, p)
        c.drawRoundRect(RectF(cx + w / 2f - 4f, top + h * 0.66f, cx + w / 2f + 6f, top + h * 0.88f), 4f, 4f, p)
        p.color = color
        c.drawRoundRect(RectF(cx - w / 2f, top, cx + w / 2f, top + h), w * 0.28f, w * 0.28f, p)
        p.color = 0xCC1A2233.toInt()
        c.drawRoundRect(RectF(cx - w * 0.36f, top + h * 0.22f, cx + w * 0.36f, top + h * 0.42f), 8f, 8f, p)
        c.drawRoundRect(RectF(cx - w * 0.36f, top + h * 0.68f, cx + w * 0.36f, top + h * 0.82f), 8f, 8f, p)
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(0xFF0A1220.toInt())
        val cx = width / 2f; val rw = roadW(); val left = cx - rw / 2f; val laneW = rw / 3f
        p.style = Paint.Style.FILL; p.color = 0xFF20242E.toInt()
        c.drawRoundRect(RectF(left, -40f, left + rw, height + 40f), 30f, 30f, p)
        p.color = 0xFFDDDDDD.toInt()
        c.drawRect(left + 6f, 0f, left + 12f, height.toFloat(), p)
        c.drawRect(left + rw - 12f, 0f, left + rw - 6f, height.toFloat(), p)
        p.color = 0x99FFFFFF.toInt()
        for (l in 1..2) {
            var y = -90f + dash
            while (y < height) { c.drawRect(left + laneW * l - 3f, y, left + laneW * l + 3f, y + 45f, p); y += 90f }
        }
        for (i in obsY.indices) car(c, left + laneW * (obsLane[i] + 0.5f), obsY[i], carW(), carH(), obsCol[i])
        car(c, left + laneW * (pxLane + 0.5f), height - carH() - 30f, carW(), carH(), 0xFFFF4D4D.toInt())
        hud(c, "Score ${score()}    Best $best    ${(speed / 4f).toInt()} km/h")
        if (waiting) panel(c, gameName, "Left / Right to steer  -  OK to start")
        else if (dead) panel(c, "Crash!", "Score ${score()}  -  Press OK to retry")
    }
}
