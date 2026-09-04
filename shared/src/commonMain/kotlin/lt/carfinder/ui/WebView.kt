package lt.carfinder.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class WebController {
    var canGoBack: () -> Boolean = { false }
    var goBack: () -> Unit = {}
    var reload: () -> Unit = {}
    var loadUrl: (String) -> Unit = {}
}

@Composable
expect fun SiteWebView(
    controller: WebController,
    startUrl: String,
    onPayload: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
)
