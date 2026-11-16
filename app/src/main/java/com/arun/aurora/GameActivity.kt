package com.arun.aurora

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager

class GameActivity : Activity() {
    private var game: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val g: GameView = when (intent.getStringExtra("game")) {
            "snake" -> SnakeView(this)
            "blocks" -> BlocksView(this)
            else -> CarView(this)
        }
        game = g
        setContentView(g)
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN && e.keyCode != KeyEvent.KEYCODE_BACK) {
            if (game?.handleKey(e.keyCode) == true) return true
        }
        return super.dispatchKeyEvent(e)
    }

    override fun onPause() { game?.pause(); super.onPause() }
    override fun onResume() { super.onResume(); game?.resume() }
}
