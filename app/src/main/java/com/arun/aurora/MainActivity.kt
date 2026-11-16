package com.arun.aurora

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var repo: AppRepository
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var status: TextView
    private lateinit var favLabel: TextView
    private lateinit var recentLabel: TextView
    private lateinit var favRow: RecyclerView
    private lateinit var recentRow: RecyclerView
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
        favAdapter = newAdapter(); recentAdapter = newAdapter(); allAdapter = newAdapter()
        setupRow(favRow, favAdapter); setupRow(recentRow, recentAdapter)
        setupRow(findViewById(R.id.allRow), allAdapter)
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

    override fun onResume() {
        super.onResume()
        loadWallpaper()
        refresh()
        handler.removeCallbacks(tick); handler.post(tick)
    }

    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }

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
