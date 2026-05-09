package com.example.doomkeyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(40))
        }
        scroll.addView(root)

        // ── Title ──────────────────────────────────────────────────────
        root.addView(TextView(this).apply {
            text = "DOOM KEYBOARD"
            setTextColor(Color.rgb(220, 60, 30))
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        // ── Setup instructions ─────────────────────────────────────────
        root.addView(section("Setup"))
        root.addView(body(
            "1. Tap 'Open Keyboard Settings' and enable Doom Keyboard.\n" +
                    "2. Tap 'Switch Input Method' and select Doom Keyboard.\n" +
                    "3. Tap the test field below to try it out."
        ))

        root.addView(button("Open Keyboard Settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(button("Switch Input Method") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })

        // ── Test field ─────────────────────────────────────────────────
        root.addView(section("Test the keyboard"))
        root.addView(body("Tap below and type. Doom plays in the background as you type."))
        root.addView(EditText(this).apply {
            hint = "Type here to test..."
            setHintTextColor(Color.argb(120, 255, 255, 255))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(60, 255, 255, 255))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                .apply { topMargin = dp(8) }
        })

        // ── Doom key map ──────────────────────────────────────────────
        root.addView(section("Doom controls"))
        root.addView(body(
            "Every key on the keyboard does TWO things:\n" +
                    "  • types its character into the text field\n" +
                    "  • sends an input to the Doom game underneath\n\n" +
                    "Mappings to default Doom controls:"
        ))
        root.addView(mappingTable(arrayOf(
            "Key"      to "Doom action",
            "W"        to "Move forward",
            "S"        to "Move back",
            "A"        to "Turn left",
            "D"        to "Turn right",
            "Q"        to "Strafe left",
            "E"        to "Strafe right",
            "1 – 7"    to "Select weapon",
            "Space"    to "USE / open door / pick up",
            "Enter ↵"  to "Menu confirm",
            "Shift ⇧"  to "Run (hold)",
            "Tab"      to "Toggle automap",
            "Esc"      to "Open menu",
            "Ctrl"     to "FIRE",
            "Alt"      to "Strafe modifier"
        )))

        root.addView(body(
            "\nAll other letters and symbols only type — they do not affect the game.\n" +
                    "Numbers 8, 9, 0 type but have no Doom binding.\n" +
                    "Hold a movement key (W/A/S/D) to keep moving in that direction."
        ))

        setContentView(scroll)
    }

    // ── Tiny UI helpers ────────────────────────────────────────────────
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun section(title: String) = TextView(this).apply {
        text = title.uppercase()
        setTextColor(Color.rgb(220, 60, 30))
        textSize = 16f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setPadding(0, dp(24), 0, dp(8))
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 14f
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            .apply { topMargin = dp(8) }
    }

    private fun mappingTable(rows: Array<Pair<String, String>>) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(8), 0, 0)
        for ((i, pair) in rows.withIndex()) {
            val isHeader = i == 0
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(8), dp(6), dp(8), dp(6))
                if (isHeader) {
                    setBackgroundColor(Color.argb(80, 220, 60, 30))
                } else if (i % 2 == 0) {
                    setBackgroundColor(Color.argb(30, 255, 255, 255))
                }
            }
            row.addView(TextView(this@MainActivity).apply {
                text = pair.first
                setTextColor(if (isHeader) Color.WHITE else Color.rgb(255, 200, 100))
                textSize = 14f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            })
            row.addView(TextView(this@MainActivity).apply {
                text = pair.second
                setTextColor(Color.WHITE)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 2f)
            })
            addView(row, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
    }
}