package com.gexetr.teachergradebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gexetr.teachergradebook.ui.PlayKilnApp
import com.gexetr.teachergradebook.ui.theme.PlayKilnTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as PlayKilnApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Play's purchase sheet needs a live activity, so it is handed over on create and taken
        // back on destroy instead of being held for the life of the process.
        container.attach(this)
        setContent {
            PlayKilnTheme {
                PlayKilnApp(container = container)
            }
        }
    }

    override fun onDestroy() {
        container.detach(this)
        super.onDestroy()
    }
}
