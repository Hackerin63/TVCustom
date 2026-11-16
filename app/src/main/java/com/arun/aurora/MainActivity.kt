package com.arun.aurora

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    companion object { private var splashDone = false }

    private lateinit var repo: AppRepository
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var status: TextView
    private lateinit var favLabel: TextView
    private lateinit var recentLabel: TextView
    private lateinit var favRow: RecyclerView
    private lateinit var recentRow: RecyclerView
    private lateinit var allRow: RecyclerView
    private lateinit var favAdapter: AppAdapter
    private lateinit var recentAdapter: AppAdapter
    private lateinit var allAdapter: AppAdapter

    private val tick = object : Runnable {
        override fun run() {
            val now = Date()
            clock.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
            date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
            status.text = networkStatus()
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = AppRepository(this)
        clock = findViewById(R.id.clock); date = findViewById(R.id.date); status = findViewById(R.id.status)
        favLabel = findViewById(R.id.favLabel); recentLabel = findViewById(R.id.recentLabel)
        favRow = findViewById(R.id.favRow); recentRow = findViewById(R.id.recentRow)
        allRow = findViewById(R.id.allRow)
        favAdapter = newAdapter(); recentAdapter = newAdapter(); allAdapter = newAdapter()
        setupRow(favRow, favAdapter); setupRow(recentRow, recentAdapter)
        allRow.layoutManager = GridLayoutManager(this, 5)
        allRow.adapter = allAdapter
        buildSettingsRow()
    }

    private fun newAdapter() = AppAdapter(
        onClick = { e ->
            repo.recordLaunch(e.pkg)
            startActivity(Intent(Intent.ACTION_MAIN).setComponent(e.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        },
        onLong = { e -> repo.toggleFavorite(e.pkg); refresh() }
    )

    private fun setupRow(rv: RecyclerView, a: AppAdapter) {
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = a
        rv.setHasFixedSize(true)
    }

    // Shortcuts into the native Settings app (original Settings is untouched)
    private fun buildSettingsRow() {
        val row = findViewById<LinearLayout>(R.id.settingsRow)
        val items = listOf(
            "Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,
            "Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
            "Display" to Settings.ACTION_DISPLAY_SETTINGS,
            "Sound" to Settings.ACTION_SOUND_SETTINGS,
            "Storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            "Apps" to Settings.ACTION_APPLICATION_SETTINGS,
            "All Settings" to Settings.ACTION_SETTINGS
        )
        val gap = (8 * resources.displayMetrics.density).toInt()
        items.forEach { (label, action) ->
            val tv = TextView(this).apply {
                text = label
                setTextColor(getColor(R.color.text_primary))
                textSize = 13f
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                setBackgroundResource(R.drawable.tile_bg)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginEnd = gap }
                setOnClickListener { openSettings(action) }
                setOnFocusChangeListener { v, f ->
                    val s = if (f) 1.08f else 1f
                    v.animate().scaleX(s).scaleY(s).setDuration(120).start()
                }
            }
            row.addView(tv)
        }
    }

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            try { startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            catch (_: Exception) { }
        }
    }

    override fun onResume() {
        super.onResume()
        loadWallpaper()
        refresh()
        handler.removeCallbacks(tick); handler.post(tick)
        if (splashDone) {
            findViewById<View>(R.id.splash).visibility = View.GONE
            animateIn()
        } else {
            playSplash()
        }
    }

    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }

    // "KPM TV" intro, plays once per boot (process start)
    private fun playSplash() {
        splashDone = true
        val s = findViewById<View>(R.id.splash)
        val t = findViewById<View>(R.id.splashText)
        s.visibility = View.VISIBLE
        s.alpha = 1f
        t.alpha = 0f; t.scaleX = 0.9f; t.scaleY = 0.9f
        t.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).withEndAction {
            s.animate().alpha(0f).setStartDelay(600).setDuration(450).withEndAction {
                s.visibility = View.GONE
                animateIn()
            }.start()
        }.start()
    }

    private fun animateIn() {
        val views = listOf<View>(findViewById(R.id.settingsRow), favRow, recentRow, allRow)
        views.forEachIndexed { i, v ->
            v.alpha = 0f; v.translationY = 30f
            v.animate().alpha(1f).translationY(0f).setStartDelay(80L * i).setDuration(350).start()
        }
    }

    private fun refresh() {
        val all = repo.allApps()
        val favs = repo.favorites(all); val rec = repo.recent(all)
        favAdapter.submit(favs); recentAdapter.submit(rec); allAdapter.submit(all)
        favLabel.visibility = if (favs.isEmpty()) View.GONE else View.VISIBLE
        favRow.visibility = favLabel.visibility
        recentLabel.visibility = if (rec.isEmpty()) View.GONE else View.VISIBLE
        recentRow.visibility = recentLabel.visibility
    }

    // Optional wallpaper: adb push wallpaper.jpg /sdcard/Android/data/com.arun.aurora/files/wallpaper.jpg
    private fun loadWallpaper() {
        val iv = findViewById<ImageView>(R.id.wallpaper)
        val f = File(getExternalFilesDir(null), "wallpaper.jpg")
        if (!f.exists()) { iv.setImageDrawable(null); return }
        val o = BitmapFactory.Options().apply { inSampleSize = 2; inPreferredConfig = Bitmap.Config.RGB_565 }
        iv.setImageBitmap(BitmapFactory.decodeFile(f.path, o))
    }

    private fun networkStatus(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "No network"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi connected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet connected"
            else -> "Online"
        }
    }

    override fun onBackPressed() { /* launcher: Back does nothing */ }
}
