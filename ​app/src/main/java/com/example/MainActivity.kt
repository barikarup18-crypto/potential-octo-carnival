package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CartoonStudioScreen
import com.example.ui.theme.CartoonStudioTheme
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val logFile = File(filesDir, "last_crash.txt")
        val previousCrash = if (logFile.exists()) {
            val txt = logFile.readText()
            logFile.delete()
            txt
        } else null

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val msg = sw.toString()
            try {
                logFile.writeText(msg)
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("Crash", msg))
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(t, e)
        }

        setContent {
            if (previousCrash != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "LAST CRASH LOG:\n\n$previousCrash",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            } else {
                CartoonStudioTheme {
                    CartoonStudioScreen()
                }
            }
        }
    }
}
