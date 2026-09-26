package com.gexetr.pipesizing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gexetr.pipesizing.monetization.PlayKilnContainer
import com.gexetr.pipesizing.ui.PlayKilnApp
import com.gexetr.pipesizing.ui.theme.PlayKilnTheme

class MainActivity : ComponentActivity() {

    private val container: PlayKilnContainer get() = (application as PlayKilnApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Consent needs a foreground Activity, and monetization starts once per process: the
        // clients themselves live on the Application so a rotation does not rebuild them.
        container.setActivity(this)
        container.startMonetization()

        setContent {
            PlayKilnTheme {
                PlayKilnApp(container = container)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.setActivity(this)
    }

    override fun onPause() {
        container.setActivity(null)
        super.onPause()
    }

    override fun onDestroy() {
        if (isFinishing) container.close()
        super.onDestroy()
    }
}
