package lt.carfinder.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import lt.carfinder.platform.AndroidPlatform
import lt.carfinder.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidPlatform.init(applicationContext)
        enableEdgeToEdge()
        setContent { App() }
    }
}
