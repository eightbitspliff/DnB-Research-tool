package com.flitz.igel

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

/**
 * Vollbild-Hülle für das HTML5-Spiel aus flitz-game/index.html.
 * Das Spiel wird über eine https-Adresse aus den App-Assets geladen, damit localStorage
 * (gespeicherter Fortschritt) und Web Audio wie in einem normalen Browser funktionieren.
 */
class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(12, 15, 48))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.textZoom = 100
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.allowFileAccess = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            // Langes Drücken = "Anhalten" im Spiel, daher kein Textauswahl-Menü.
            isLongClickable = false
            isHapticFeedbackEnabled = false
            setOnLongClickListener { true }
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    assetLoader.shouldInterceptRequest(request.url)
            }
        }
        setContentView(webView)

        // Wisch-Gesten am linken/rechten Rand sollen das Spiel steuern statt "Zurück" auszulösen.
        // Android erlaubt pro Rand höchstens 200dp Ausnahmehöhe, mittig platziert.
        webView.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val h = (200 * resources.displayMetrics.density).toInt().coerceAtMost(v.height)
            val edge = (48 * resources.displayMetrics.density).toInt()
            val top = (v.height - h) / 2
            v.systemGestureExclusionRects = listOf(
                Rect(0, top, edge, top + h),
                Rect(v.width - edge, top, v.width, top + h),
            )
        }

        if (savedInstanceState != null) webView.restoreState(savedInstanceState)
        else webView.loadUrl(GAME_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Zurück-Geste: im Spiel pausieren, in Menüs zum Titel, auf dem Titel App schließen.
                webView.evaluateJavascript("window.__flitz ? window.__flitz.back() : false") { handled ->
                    if (handled != "true") finish()
                }
            }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onPause() {
        webView.evaluateJavascript("window.__flitz && window.__flitz.pause()", null)
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        hideSystemBars()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val GAME_URL = "https://appassets.androidplatform.net/assets/index.html"
    }
}
