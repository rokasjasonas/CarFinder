package lt.carfinder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import lt.carfinder.sites.Sites
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.darwin.DISPATCH_TIME_NOW

private class Bridge(
    private val onPayload: (String) -> Unit,
    private val onUrlChanged: (String) -> Unit,
) : NSObject(), WKScriptMessageHandlerProtocol, WKNavigationDelegateProtocol {

    override fun userContentController(userContentController: WKUserContentController, didReceiveScriptMessage: WKScriptMessage) {
        (didReceiveScriptMessage.body as? String)?.let(onPayload)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        webView.URL?.absoluteString?.let(onUrlChanged)
        listOf(0L, 1500L, 4000L, 8000L).forEach { delay ->
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delay * 1_000_000L), dispatch_get_main_queue()) {
                webView.evaluateJavaScript(Sites.extractorJs, null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun SiteWebView(
    controller: WebController,
    startUrl: String,
    onPayload: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier,
) {
    val bridge = remember { Bridge(onPayload, onUrlChanged) }
    UIKitView(
        modifier = modifier,
        factory = {
            val config = WKWebViewConfiguration().apply {
                userContentController.addScriptMessageHandler(bridge, Sites.BRIDGE)
            }
            WKWebView(frame = platform.CoreGraphics.CGRectZero.readValue(), configuration = config).apply {
                navigationDelegate = bridge
                controller.canGoBack = { canGoBack }
                controller.goBack = { goBack() }
                controller.reload = { reload() }
                controller.loadUrl = { url -> NSURL.URLWithString(url)?.let { loadRequest(NSURLRequest.requestWithURL(it)) } }
                NSURL.URLWithString(startUrl)?.let { loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        },
    )
}
