package lt.carfinder.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import lt.carfinder.sites.Sites

private class Bridge(private val onPayload: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onListing(json: String) {
        main.post { onPayload(json) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun SiteWebView(
    controller: WebController,
    startUrl: String,
    onPayload: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier,
) {
    val bridge = remember { Bridge(onPayload) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                addJavascriptInterface(bridge, Sites.BRIDGE)
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        url?.let(onUrlChanged)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        url?.let(onUrlChanged)
                        // Lazy-rendered card lists need a few passes before all listings are in the DOM.
                        listOf(0L, 1500L, 4000L, 8000L).forEach { delay ->
                            view.postDelayed({ view.evaluateJavascript(Sites.extractorJs, null) }, delay)
                        }
                    }

                    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                        url?.let(onUrlChanged)
                        view.evaluateJavascript(Sites.extractorJs, null)
                    }
                }
                controller.canGoBack = { canGoBack() }
                controller.goBack = { goBack() }
                controller.reload = { reload() }
                controller.loadUrl = { loadUrl(it) }
                loadUrl(startUrl)
            }
        },
    )
}
