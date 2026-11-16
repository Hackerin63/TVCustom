package com.arun.aurora

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.text.InputType
import android.graphics.drawable.GradientDrawable

class BrowserActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var url: EditText

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val acc = ThemeManager.current(this).accent
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0B0F1A.toInt())
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(this@BrowserActivity, 16), Ui.dp(this@BrowserActivity, 8),
                Ui.dp(this@BrowserActivity, 16), Ui.dp(this@BrowserActivity, 8))
        }
        fun btn(label: String, onClick: () -> Unit) = Ui.button(this, label, acc, onClick).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { marginEnd = Ui.dp(this@BrowserActivity, 8) }
        }

        url = EditText(this).apply {
            hint = "Search Google or type a website"
            setHintTextColor(0x88FFFFFF.toInt())
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setPadding(Ui.dp(this@BrowserActivity, 18), Ui.dp(this@BrowserActivity, 10),
                Ui.dp(this@BrowserActivity, 18), Ui.dp(this@BrowserActivity, 10))
            background = GradientDrawable().apply {
                cornerRadius = Ui.dp(this@BrowserActivity, 22).toFloat()
                setColor(0x33FFFFFF)
            }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = Ui.dp(this@BrowserActivity, 8) }
            setOnEditorActionListener { _, actionId, ev ->
                if (actionId == EditorInfo.IME_ACTION_GO || (ev != null && ev.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    go(text.toString()); true
                } else false
            }
        }

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.builtInZoomControls = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, u: String) { url.setText(u) }
            }
        }

        bar.addView(btn("Back") { if (web.canGoBack()) web.goBack() })
        bar.addView(btn("Next") { if (web.canGoForward()) web.goForward() })
        bar.addView(btn("Reload") { web.reload() })
        bar.addView(btn("Google") { web.loadUrl("https://www.google.com") })
        bar.addView(url)
        bar.addView(btn("Go") { go(url.text.toString()) })

        root.addView(bar)
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        web.loadUrl("https://www.google.com")
        web.requestFocus()
    }

    private fun go(q: String) {
        val s = q.trim()
        if (s.isEmpty()) return
        val target = when {
            s.startsWith("http://") || s.startsWith("https://") -> s
            s.contains(" ") || !s.contains(".") -> "https://www.google.com/search?q=" + Uri.encode(s)
            else -> "https://$s"
        }
        web.loadUrl(target)
        web.requestFocus()
    }

    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
    override fun onDestroy() { web.destroy(); super.onDestroy() }
}
