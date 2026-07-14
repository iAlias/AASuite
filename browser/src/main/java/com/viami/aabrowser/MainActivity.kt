package com.viami.aabrowser

import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        setContentView(
            TextView(this).apply {
                setPadding(padding, padding, padding, padding)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                text = getString(R.string.phone_instructions)
            }
        )
    }
}
