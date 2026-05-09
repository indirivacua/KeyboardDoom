package com.example.doomkeyboard

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout

class DoomKeyboardService : InputMethodService() {

    private var webView: WebView? = null
    private var keys: KeyGridView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateInputView(): View {
        val heightPx = (300 * resources.displayMetrics.density).toInt()

        // The same height-forcing wrapper from before.
        val wrapper = object : FrameLayout(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val forcedHeightSpec = MeasureSpec.makeMeasureSpec(
                    heightPx, MeasureSpec.EXACTLY
                )
                super.onMeasure(widthMeasureSpec, forcedHeightSpec)
            }
        }
        wrapper.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            heightPx
        )

        layoutInflater.inflate(R.layout.keyboard, wrapper, true)

        webView = wrapper.findViewById<WebView>(R.id.doomWeb).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = true

            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false

            android.webkit.WebView.setWebContentsDebuggingEnabled(true)

            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(msg: android.webkit.ConsoleMessage): Boolean {
                    android.util.Log.d("DoomWeb",
                        "${msg.messageLevel()}: ${msg.message()} (line ${msg.lineNumber()})")
                    return true
                }
            }

            // Direct file:// load — the deprecated allowFileAccess* settings above
            // let the page reach sibling assets without CORS errors.
            loadUrl("file:///android_asset/doom.html")
        }

        keys = wrapper.findViewById(R.id.keys)

        // Typing-mode callbacks (unchanged behavior).
        keys?.onKey = { text ->
            currentInputConnection?.commitText(text, 1)
        }
        keys?.onBackspace = {
            val ic = currentInputConnection
            if (ic != null) {
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) ic.commitText("", 1)
                else ic.deleteSurroundingText(1, 0)
            }
        }
        keys?.onEnter = {
            currentInputConnection?.commitText("\n", 1)
        }

        // Game-mode callback: route key presses into Doom via JavaScript.
        keys?.onGameKey = { jsKeyCode, isDown ->
            webView?.evaluateJavascript(
                "dispatchKey($jsKeyCode, ${if (isDown) "true" else "false"});",
                null
            )
        }

        return wrapper
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        webView?.onResume()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // Pauses the JS engine — saves battery while keyboard is hidden.
        webView?.onPause()
    }

    override fun onDestroy() {
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}