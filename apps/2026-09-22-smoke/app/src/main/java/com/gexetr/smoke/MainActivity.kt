package com.gexetr.smoke

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gexetr.smoke.ui.PlayKilnApp
import com.gexetr.smoke.ui.theme.PlayKilnTheme

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
