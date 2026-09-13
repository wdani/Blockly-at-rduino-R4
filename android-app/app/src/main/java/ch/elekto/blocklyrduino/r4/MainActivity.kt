package ch.elekto.blocklyrduino.r4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ch.elekto.blocklyrduino.r4.ui.ElektoApp
import ch.elekto.blocklyrduino.r4.ui.ElektoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElektoTheme {
                ElektoApp()
            }
        }
    }
}
