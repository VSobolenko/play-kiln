package com.gexetr.pipesizing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gexetr.pipesizing.ui.PlayKilnApp
import com.gexetr.pipesizing.ui.theme.PlayKilnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayKilnTheme {
                PlayKilnApp()
            }
        }
    }
}
