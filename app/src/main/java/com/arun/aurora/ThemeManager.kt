package com.arun.aurora

import android.content.Context

enum class Theme(val key: String, val title: String, val accent: Int) {
    AURORA("aurora", "Aurora Night", 0xFF6EA8FF.toInt()),
    SNOW("snow", "Snowfall", 0xFF9AD4FF.toInt()),
    CARS("cars", "Night Racing", 0xFFFF4D4D.toInt()),
    FIRE("fire", "Fire", 0xFFFF8A3D.toInt()),
    PHOTO("photo", "My Photo", 0xFFFFD166.toInt())
}

object ThemeManager {
    fun current(ctx: Context): Theme {
        val k = ctx.getSharedPreferences("aurora", Context.MODE_PRIVATE).getString("theme", "aurora")
        return Theme.values().firstOrNull { it.key == k } ?: Theme.AURORA
    }

    fun set(ctx: Context, t: Theme) {
        ctx.getSharedPreferences("aurora", Context.MODE_PRIVATE).edit().putString("theme", t.key).apply()
    }
}

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("aurora", Context.MODE_PRIVATE)
    fun animated(c: Context) = sp(c).getBoolean("anim", true)
    fun setAnimated(c: Context, v: Boolean) = sp(c).edit().putBoolean("anim", v).apply()
    fun sounds(c: Context) = sp(c).getBoolean("sounds", false)
    fun setSounds(c: Context, v: Boolean) = sp(c).edit().putBoolean("sounds", v).apply()
    fun intro(c: Context) = sp(c).getBoolean("intro", true)
    fun setIntro(c: Context, v: Boolean) = sp(c).edit().putBoolean("intro", v).apply()
}
