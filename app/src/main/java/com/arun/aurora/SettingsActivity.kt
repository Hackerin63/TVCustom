package com.arun.aurora

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var cur = ThemeManager.current(this)
        val match = ViewGroup.LayoutParams.MATCH_PARENT
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

        // Animated wallpaper shows through the translucent cards
        val root = FrameLayout(this)
        val wp = LiveWallpaperView(this)
        wp.animated = Prefs.animated(this)
        wp.setTheme(cur)
        root.addView(wp, FrameLayout.LayoutParams(match, match))
        root.addView(View(this).apply { setBackgroundColor(0x22000000) }, FrameLayout.LayoutParams(match, match))

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@SettingsActivity, 48), Ui.dp(this@SettingsActivity, 24),
                Ui.dp(this@SettingsActivity, 48), Ui.dp(this@SettingsActivity, 24))
        }

        fun title(s: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            setPadding(Ui.dp(this@SettingsActivity, 4), Ui.dp(this@SettingsActivity, 4), 0, Ui.dp(this@SettingsActivity, 8))
        }

        fun card(heading: String): LinearLayout {
            val c = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val pad = Ui.dp(this@SettingsActivity, 16)
                setPadding(pad, pad, pad, pad)
                background = Ui.cardBg(this@SettingsActivity)
            }
            c.addView(title(heading, 15f, 0xFFB8C2DD.toInt()))
            col.addView(c, LinearLayout.LayoutParams(match, wrap).apply { bottomMargin = Ui.dp(this@SettingsActivity, 14) })
            return c
        }

        fun rowOf(parent: LinearLayout): LinearLayout {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            parent.addView(r, LinearLayout.LayoutParams(match, wrap))
            return r
        }

        fun cell(v: View) {
            v.layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
                .apply { marginEnd = Ui.dp(this@SettingsActivity, 10); bottomMargin = Ui.dp(this@SettingsActivity, 10) }
        }

        col.addView(title("KPM TV  -  Launcher Settings", 28f))

        // 1. Wallpaper themes
        val themeCard = card("Wallpaper theme")
        val themeBtns = ArrayList<Pair<Theme, TextView>>()
        fun relabel() { themeBtns.forEach { (t, b) -> b.text = (if (t == cur) "● " else "") + t.title } }
        val themeRow = rowOf(themeCard)
        Theme.values().forEach { th ->
            val b = Ui.button(this, th.title, th.accent) {
                cur = th
                ThemeManager.set(this, th)
                wp.setTheme(th)
                relabel()
            }
            cell(b); themeBtns.add(th to b); themeRow.addView(b)
        }
        relabel()
        themeCard.addView(title("My Photo: copy wallpaper.jpg to /sdcard/Android/data/com.arun.aurora/files/ (adb push), then pick My Photo.", 12f, 0xFF8A94B0.toInt()))

        // 2. Launcher options
        val optCard = card("Launcher")
        val optRow = rowOf(optCard)
        fun toggle(name: String, get: () -> Boolean, set: (Boolean) -> Unit, after: () -> Unit = {}) {
            lateinit var b: TextView
            fun lbl() { b.text = "$name: " + if (get()) "ON" else "OFF" }
            b = Ui.button(this, "", cur.accent) { set(!get()); lbl(); after() }
            lbl(); cell(b); optRow.addView(b)
        }
        toggle("Wallpaper animation", { Prefs.animated(this) }, { Prefs.setAnimated(this, it) }) { wp.setAnimatedFlag(Prefs.animated(this)) }
        toggle("Menu sounds", { Prefs.sounds(this) }, { Prefs.setSounds(this, it) })
        toggle("KPM TV intro", { Prefs.intro(this) }, { Prefs.setIntro(this, it) })

        // 3. System settings shortcuts
        val sysCard = card("System settings (opens the original TV Settings)")
        val items = listOf(
            "Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,
            "Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
            "Display" to Settings.ACTION_DISPLAY_SETTINGS,
            "Sound" to Settings.ACTION_SOUND_SETTINGS,
            "Storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            "Apps" to Settings.ACTION_APPLICATION_SETTINGS,
            "Developer" to Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            "All Settings" to Settings.ACTION_SETTINGS
        )
        var row = rowOf(sysCard)
        items.forEachIndexed { i, (label, action) ->
            if (i > 0 && i % 4 == 0) row = rowOf(sysCard)
            val b = Ui.button(this, label, cur.accent) { Ui.openSettings(this, action) }
            cell(b); row.addView(b)
        }

        val scroll = ScrollView(this)
        scroll.addView(col)
        root.addView(scroll, FrameLayout.LayoutParams(match, match))
        setContentView(root)
        themeBtns.firstOrNull()?.second?.requestFocus()
    }
}
