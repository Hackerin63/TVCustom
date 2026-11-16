package com.arun.aurora

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    companion object { private var splashDone = false }

    private enum class Cat(val title: String) {
        HOME("Home"), MEDIA("Media"), GAMES("Games"), TOOLS("Tools"), ALL("All Apps")
    }

    private lateinit var repo: AppRepository
    private lateinit var wallpaper: LiveWallpaperView
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var topIcons: LinearLayout
    private lateinit var wifiIcon: RoundIconView
    private lateinit var btIcon: RoundIconView
    private val topIconList = ArrayList<RoundIconView>()
    private lateinit var catRow: LinearLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvSub: TextView
    private lateinit var panel: View
    private lateinit var slider: RecyclerView
    private lateinit var adapter: ItemAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val pills = ArrayList<Pair<Cat, TextView>>()
    private var currentCat = Cat.HOME
    private var shownTheme: Theme? = null
    private var accent = 0xFF6EA8FF.toInt()
    private var loaded = false
    private var lastIds: List<String> = emptyList()

    private val tick = object : Runnable {
        override fun run() {
            val now = Date()
            clock.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
            date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
            wifiIcon.active = networkUp()
            btIcon.active = btOn()
            handler.postDelayed(this, 20_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = AppRepository(this)
        wallpaper = findViewById(R.id.wallpaper)
        clock = findViewById(R.id.clock); date = findViewById(R.id.date)
        topIcons = findViewById(R.id.topIcons); catRow = findViewById(R.id.catRow)
        tvTitle = findViewById(R.id.tvTitle); tvSub = findViewById(R.id.tvSub)
        panel = findViewById(R.id.panel); slider = findViewById(R.id.slider)

        slider.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        slider.itemAnimator = null
        slider.setHasFixedSize(true)
        slider.setItemViewCacheSize(8)
        val side = resources.displayMetrics.widthPixels / 2 - Ui.dp(this, 80)
        slider.setPadding(side, Ui.dp(this, 14), side, Ui.dp(this, 14))
        adapter = ItemAdapter({ onItem(it) }, { onItemLong(it) }, { i, v -> onItemFocus(i, v) })
        slider.adapter = adapter

        buildTopIcons()
        buildPills()
    }

    // ---------- top bar: small round icons ----------
    private fun addIcon(g: Glyph, onClick: () -> Unit): RoundIconView {
        val v = RoundIconView(this, g)
        v.setOnClickListener { onClick() }
        topIcons.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { marginStart = Ui.dp(this@MainActivity, 12) })
        topIconList.add(v)
        return v
    }

    private fun buildTopIcons() {
        addIcon(Glyph.GLOBE) { startActivity(Intent(this, BrowserActivity::class.java)) }
        wifiIcon = addIcon(Glyph.WIFI) { Ui.openSettings(this, Settings.ACTION_WIFI_SETTINGS) }
        btIcon = addIcon(Glyph.BLUETOOTH) { Ui.openSettings(this, Settings.ACTION_BLUETOOTH_SETTINGS) }
        addIcon(Glyph.GEAR) { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    // ---------- category pills ----------
    private fun buildPills() {
        for (cat in Cat.values()) {
            val tv = TextView(this)
            tv.text = cat.title
            tv.textSize = 14f
            tv.gravity = Gravity.CENTER
            tv.setTextColor(0xFFFFFFFF.toInt())
            tv.setPadding(Ui.dp(this, 18), Ui.dp(this, 7), Ui.dp(this, 18), Ui.dp(this, 7))
            tv.isFocusable = true
            tv.isClickable = true
            tv.id = View.generateViewId()
            tv.setOnClickListener { setCategory(cat) }
            tv.setOnFocusChangeListener { v, f ->
                stylePill(tv, cat, f)
                val s = if (f) 1.08f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(150).setInterpolator(DecelerateInterpolator()).start()
                if (f) { setCategory(cat); click() }
            }
            catRow.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = Ui.dp(this@MainActivity, 5); marginEnd = Ui.dp(this@MainActivity, 5) })
            pills.add(cat to tv)
            stylePill(tv, cat, false)
        }
    }

    private fun stylePill(tv: TextView, cat: Cat, focused: Boolean) {
        val sel = cat == currentCat
        tv.background = GradientDrawable().apply {
            cornerRadius = Ui.dp(this@MainActivity, 20).toFloat()
            setColor(when {
                focused -> 0x55FFFFFF
                sel -> (accent and 0x00FFFFFF) or 0x66000000
                else -> 0x22FFFFFF
            })
            if (focused) setStroke(Ui.dp(this@MainActivity, 2), accent)
        }
    }

    private fun updatePills() { pills.forEach { (c, tv) -> stylePill(tv, c, tv.hasFocus()) } }

    // ---------- items ----------
    private val browserItem = Item("browser", "Browser", badge = "W", c1 = 0xFF4285F4.toInt(), c2 = 0xFF1A3A8A.toInt())
    private val snakeItem = Item("snake", "Snake", badge = "S", c1 = 0xFF2ECC71.toInt(), c2 = 0xFF0B6B3A.toInt())
    private val blocksItem = Item("blocks", "Blocks", badge = "B", c1 = 0xFFB388FF.toInt(), c2 = 0xFF4A2A9A.toInt())
    private val carsItem = Item("cars", "Car Race", badge = "R", c1 = 0xFFFF5252.toInt(), c2 = 0xFF8A1A1A.toInt())
    private val settingsItem = Item("themes", "KPM Settings", badge = "K", c1 = 0xFF6EA8FF.toInt(), c2 = 0xFF1B2440.toInt())

    private fun catOf(a: AppEntry): Cat {
        val ac = try { packageManager.getApplicationInfo(a.pkg, 0).category } catch (_: Exception) { -1 }
        val p = a.pkg.lowercase()
        val mediaWords = listOf("youtube", "vlc", "netflix", "video", "music", "livetv", "iptv", "player",
            "plex", "kodi", "indiatv", "flixfox", "mediatek.wwtv")
        return when {
            ac == ApplicationInfo.CATEGORY_GAME || p.contains("game") -> Cat.GAMES
            ac == ApplicationInfo.CATEGORY_VIDEO || ac == ApplicationInfo.CATEGORY_AUDIO ||
                ac == ApplicationInfo.CATEGORY_IMAGE || mediaWords.any { p.contains(it) } -> Cat.MEDIA
            else -> Cat.TOOLS
        }
    }

    private fun itemsFor(cat: Cat): List<Item> {
        val all = repo.allApps()
        fun app(a: AppEntry) = Item(a.pkg, a.label, app = a)
        return when (cat) {
            Cat.HOME -> listOf(browserItem, snakeItem, blocksItem, carsItem, settingsItem) +
                repo.favorites(all).map { app(it) }
            Cat.MEDIA -> all.filter { catOf(it) == Cat.MEDIA }.map { app(it) }
            Cat.GAMES -> listOf(snakeItem, blocksItem, carsItem) + all.filter { catOf(it) == Cat.GAMES }.map { app(it) }
            Cat.TOOLS -> all.filter { catOf(it) == Cat.TOOLS }.map { app(it) }
            Cat.ALL -> all.map { app(it) }
        }
    }

    private fun setCategory(cat: Cat, force: Boolean = false, swap: Boolean = true) {
        if (!force && cat == currentCat && loaded) return
        currentCat = cat
        updatePills()
        adapter.upFocusId = pills.first { it.first == cat }.second.id
        val list = itemsFor(cat)
        val ids = list.map { it.id }
        if (!swap && loaded && ids == lastIds) return
        lastIds = ids
        tvTitle.text = cat.title
        tvSub.text = "${list.size} items"
        if (!loaded || !swap || !Prefs.animated(this)) {
            loaded = true
            adapter.submit(list)
            if (swap) slider.scrollToPosition(0)
            return
        }
        slider.animate().cancel()
        slider.animate().alpha(0f).setDuration(90).withEndAction {
            adapter.submit(list)
            slider.scrollToPosition(0)
            slider.animate().alpha(1f).setDuration(170).start()
        }.start()
    }

    // ---------- clicks ----------
    private fun onItem(item: Item) {
        when (item.id) {
            "snake", "blocks", "cars" -> startActivity(Intent(this, GameActivity::class.java).putExtra("game", item.id))
            "browser" -> startActivity(Intent(this, BrowserActivity::class.java))
            "themes" -> startActivity(Intent(this, SettingsActivity::class.java))
            else -> item.app?.let { launch(it) }
        }
    }

    private fun onItemLong(item: Item) { item.app?.let { showAppMenu(it) } }

    private fun onItemFocus(item: Item, v: View) {
        tvTitle.text = item.label
        tvSub.text = item.app?.let { catOf(it).title } ?: "Built-in"
        click()
        val dx = (v.left + v.width / 2) - slider.width / 2
        slider.smoothScrollBy(dx, 0, DecelerateInterpolator(), 240)
    }

    private fun click() {
        if (Prefs.sounds(this)) {
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_RIGHT)
        }
    }

    private fun launch(e: AppEntry) {
        repo.recordLaunch(e.pkg)
        startActivity(Intent(Intent.ACTION_MAIN).setComponent(e.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
    }

    // Hold OK on an app -> rounded pop-up menu
    private fun showAppMenu(e: AppEntry) {
        val isFav = repo.isFavorite(e.pkg)
        val isPureSystem = try {
            val f = packageManager.getApplicationInfo(e.pkg, 0).flags
            (f and ApplicationInfo.FLAG_SYSTEM) != 0 && (f and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        } catch (_: Exception) { true }

        val labels = mutableListOf("Open", if (isFav) "Remove from favorites" else "Add to favorites", "App info")
        val actions = mutableListOf<() -> Unit>(
            { launch(e) },
            { repo.toggleFavorite(e.pkg); setCategory(currentCat, force = true, swap = false) },
            { openAppInfo(e.pkg) }
        )
        if (!isPureSystem) { labels.add("Uninstall"); actions.add { uninstall(e.pkg) } }
        RoundDialog.show(this, e.label, labels, accent) { actions[it]() }
    }

    private fun openAppInfo(pkg: String) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { Ui.openSettings(this, Settings.ACTION_APPLICATION_SETTINGS) }
    }

    private fun uninstall(pkg: String) {
        try {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) { openAppInfo(pkg) }
    }

    // ---------- lifecycle ----------
    override fun onResume() {
        super.onResume()
        val t = ThemeManager.current(this)
        if (t != shownTheme) {
            shownTheme = t
            wallpaper.setTheme(t)
            accent = t.accent
            adapter.accent = t.accent
            topIconList.forEach { it.accent = t.accent; it.invalidate() }
            updatePills()
            lastIds = emptyList()
        }
        wallpaper.setAnimatedFlag(Prefs.animated(this))
        setCategory(currentCat, force = true, swap = false)
        handler.removeCallbacks(tick); handler.post(tick)
        slider.post { if (!window.decorView.hasFocus() || currentFocus == null) slider.getChildAt(0)?.requestFocus() }

        val splash = findViewById<View>(R.id.splash)
        if (splashDone || !Prefs.intro(this)) {
            splash.visibility = View.GONE
            if (!splashDone) { splashDone = true; animateIn() } else animateIn()
        } else {
            playSplash()
        }
    }

    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }

    private fun playSplash() {
        splashDone = true
        val s = findViewById<View>(R.id.splash)
        val t = findViewById<View>(R.id.splashText)
        s.visibility = View.VISIBLE; s.alpha = 1f
        panel.alpha = 0f
        t.alpha = 0f; t.scaleX = 0.9f; t.scaleY = 0.9f
        t.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).setInterpolator(DecelerateInterpolator()).withEndAction {
            s.animate().alpha(0f).setStartDelay(600).setDuration(450).withEndAction {
                s.visibility = View.GONE
                animateIn()
            }.start()
        }.start()
    }

    private fun animateIn() {
        if (!Prefs.animated(this)) { panel.alpha = 1f; panel.translationY = 0f; topIcons.alpha = 1f; return }
        panel.alpha = 0f; panel.translationY = 120f
        panel.animate().alpha(1f).translationY(0f).setDuration(420).setInterpolator(DecelerateInterpolator()).start()
        topIcons.alpha = 0f
        topIcons.animate().alpha(1f).setStartDelay(150).setDuration(350).start()
    }

    private fun networkUp(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    @Suppress("DEPRECATION")
    private fun btOn(): Boolean = try { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true } catch (_: Exception) { false }

    override fun onBackPressed() { /* launcher: Back does nothing */ }
}
