package com.example.doomkeyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class KeyGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onKey: ((String) -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onEnter: (() -> Unit)? = null
    /** Sent for EVERY key. (browser keyCode, isDown). null is mapped — see gameKeyCode. */
    var onGameKey: ((Int, Boolean) -> Unit)? = null

    private var shifted = false

    private val rows = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l","ñ"),
        listOf("⇧","z","x","c","v","b","n","m",",",".","⌫"),
        listOf("ESC","TAB","CTRL","ALT","␣","↵")
    )

    /**
     * Browser KeyboardEvent.keyCode for the matching Doom action.
     * null = key has no Doom action (still types normally).
     * Default Doom controls assumed.
     */
    private fun gameKeyCode(label: String): Int? = when (label) {
        // Numbers — 1..7 select weapons, 8/9/0 do nothing in Doom but still send.
        "1" -> 49; "2" -> 50; "3" -> 51; "4" -> 52
        "5" -> 53; "6" -> 54; "7" -> 55; "8" -> 56
        "9" -> 57; "0" -> 48
        // WASD = movement (mapped to Doom's default arrow-key controls)
        "w" -> 38   // ArrowUp    -> forward
        "a" -> 37   // ArrowLeft  -> turn left
        "s" -> 40   // ArrowDown  -> back
        "d" -> 39   // ArrowRight -> turn right
        // QE = strafe (Doom's default ',' and '.')
        "q" -> 188  // strafe left
        "e" -> 190  // strafe right
        // Action keys
        "l"     -> 116 // L     -> toggle detail (send F5)
        "␣"     -> 87  // Space -> USE (send W)
        "↵"     -> 13  // Enter -> SELECT
        "⇧"     -> 16  // Shift -> RUN modifier
        "TAB"   -> 9   // Tab   -> AUTOMAP
        "ESC"   -> 27  // Esc   -> MENU
        "CTRL"  -> 83  // Ctrl  -> FIRE (send S)
        "ALT"   -> 18  // Alt   -> strafe modifier
        else -> null
    }

    private val keyRects = mutableListOf<KeyRect>()

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 20, 20, 20)
    }
    private val keyPaintPressed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 220, 60, 30)
    }
    private val keyBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 200, 100)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private data class KeyRect(
        val rect: RectF,
        val label: String,
        var pressed: Boolean = false
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutKeys()
    }

    private fun layoutKeys() {
        keyRects.clear()
        if (width == 0 || height == 0) return
        val pad = 4f
        val rowH = height / rows.size.toFloat()
        textPaint.textSize = rowH * 0.34f

        for ((rowIndex, row) in rows.withIndex()) {
            val weights = row.map { weightOf(it) }
            val totalWeight = weights.sum()
            val unit = (width - pad * (row.size + 1)) / totalWeight
            var x = pad
            val y = rowIndex * rowH + pad
            for ((i, label) in row.withIndex()) {
                val w = unit * weights[i]
                keyRects.add(KeyRect(RectF(x, y, x + w, y + rowH - pad * 2), label))
                x += w + pad
            }
        }
    }

    private fun weightOf(label: String): Float = when (label) {
        "␣" -> 4f
        "⇧", "⌫", "↵" -> 1.4f
        "ESC", "TAB", "CTRL", "ALT" -> 1.4f
        else -> 1f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val r = 14f
        for (k in keyRects) {
            val paint = if (k.pressed) keyPaintPressed else keyPaint
            canvas.drawRoundRect(k.rect, r, r, paint)
            canvas.drawRoundRect(k.rect, r, r, keyBorder)
            val display = displayLabel(k.label)
            val cx = k.rect.centerX()
            val cy = k.rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.save()
            canvas.clipRect(k.rect)
            canvas.drawText(display, cx, cy, textPaint)
            canvas.restore()
        }
    }

    private fun displayLabel(label: String): String = when {
        label.length == 1 && label[0].isLetter() && shifted -> label.uppercase()
        label == "ñ" && shifted -> "Ñ"
        label == "␣" -> "space"
        else -> label
    }

    private fun keyAt(x: Float, y: Float): KeyRect? =
        keyRects.firstOrNull { it.rect.contains(x, y) }

    private var pressedKey: KeyRect? = null
    private var pressedJsKey: Int? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> startKey(keyAt(event.x, event.y))
            MotionEvent.ACTION_MOVE -> {
                val k = keyAt(event.x, event.y)
                if (k != pressedKey) {
                    releaseGameKey()
                    pressedKey?.pressed = false
                    startKey(k)
                }
            }
            MotionEvent.ACTION_UP -> {
                releaseGameKey()
                pressedKey?.let { fireType(it.label) }
                pressedKey?.pressed = false
                pressedKey = null
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                releaseGameKey()
                pressedKey?.pressed = false
                pressedKey = null
                invalidate()
            }
            else -> return super.onTouchEvent(event)
        }
        return true
    }

    private fun startKey(k: KeyRect?) {
        pressedKey = k
        k?.also {
            it.pressed = true
            invalidate()
            gameKeyCode(it.label)?.let { code ->
                pressedJsKey = code
                onGameKey?.invoke(code, true)  // game keydown
            }
        }
    }

    private fun releaseGameKey() {
        pressedJsKey?.let { onGameKey?.invoke(it, false) }
        pressedJsKey = null
    }

    /** Handles the "regular keyboard" side of the press — typing into the text field. */
    private fun fireType(label: String) {
        when (label) {
            "⌫" -> onBackspace?.invoke()
            "↵" -> onEnter?.invoke()
            "⇧" -> { shifted = !shifted; invalidate() }
            "␣" -> onKey?.invoke(" ")
            "TAB" -> onKey?.invoke("\t")
            // Pure modifier / control keys — no typing equivalent.
            "ESC", "CTRL", "ALT" -> { /* game-only */ }
            else -> {
                val out = if (shifted && label.length == 1 && (label[0].isLetter() || label == "ñ"))
                    label.uppercase() else label
                onKey?.invoke(out)
                if (shifted) { shifted = false; invalidate() }
            }
        }
    }
}