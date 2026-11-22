package com.example

import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.CartoonStudioScreen
import com.example.ui.theme.CartoonStudioTheme
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val msg = sw.toString()
            runOnUiThread {
                val tv = TextView(this).apply {
                    text = "APP CRASH LOG:\n\n$msg"
                    setTextColor(Color.RED)
                    setBackgroundColor(Color.WHITE)
                    setPadding(32, 64, 32, 32)
                    textSize = 12f
                }
                setContentView(ScrollView(this).apply { addView(tv) })
            }
            try { Thread.sleep(60000) } catch (_: Exception) {}
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CartoonStudioTheme {
                CartoonStudioScreen()
            }
        }
    }
}
