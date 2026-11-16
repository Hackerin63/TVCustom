package com.arun.aurora

import android.content.ComponentName
import android.content.Context
import android.content.Intent

data class AppEntry(val pkg: String, val label: String, val component: ComponentName)

// One tile in the sliding menu: either an installed app or a built-in entry (game / themes)
data class Item(
    val id: String,
    val label: String,
    val app: AppEntry? = null,
    val badge: String? = null,
    val c1: Int = 0,
    val c2: Int = 0
)

class AppRepository(private val ctx: Context) {
    private val pm = ctx.packageManager
    private val prefs = ctx.getSharedPreferences("aurora", Context.MODE_PRIVATE)

    fun allApps(): List<AppEntry> {
        val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val normal = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return (pm.queryIntentActivities(leanback, 0) + pm.queryIntentActivities(normal, 0))
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString(),
                ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
            .sortedBy { it.label.lowercase() }
    }

    private fun favs(): Set<String> = prefs.getStringSet("favs", emptySet()) ?: emptySet()
    private fun recents(): List<String> =
        prefs.getString("recents", "")!!.split(",").filter { it.isNotBlank() }

    fun isFavorite(pkg: String) = pkg in favs()

    fun favorites(all: List<AppEntry>) = all.filter { it.pkg in favs() }
    fun recent(all: List<AppEntry>): List<AppEntry> =
        recents().mapNotNull { p -> all.firstOrNull { it.pkg == p } }

    fun toggleFavorite(pkg: String) {
        val s = favs().toMutableSet()
        if (!s.add(pkg)) s.remove(pkg)
        prefs.edit().putStringSet("favs", s).apply()
    }

    fun recordLaunch(pkg: String) {
        val l = (listOf(pkg) + recents().filter { it != pkg }).take(10)
        prefs.edit().putString("recents", l.joinToString(",")).apply()
    }
}
