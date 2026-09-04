package lt.carfinder

import androidx.compose.ui.window.ComposeUIViewController
import lt.carfinder.ui.App
import platform.UIKit.UIViewController

@Suppress("unused", "FunctionName")
fun MainViewController(): UIViewController =
    ComposeUIViewController { App() }
