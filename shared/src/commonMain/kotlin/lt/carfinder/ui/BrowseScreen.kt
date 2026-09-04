package lt.carfinder.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lt.carfinder.AppViewModel
import lt.carfinder.sites.Sites

@Composable
fun BrowseScreen(vm: AppViewModel, visible: Boolean = true) {
    val harvested = vm.state.listings.size
    val swiped = vm.swipedIds.size
    val controller = remember { WebController() }
    Column(if (visible) Modifier.fillMaxSize() else Modifier.size(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Sites.ALL.forEach { site ->
                FilterChip(
                    selected = vm.browseSite.source == site.source,
                    onClick = { controller.loadUrl(site.defaultSearch) },
                    label = { Text(site.label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(
                "$harvested harvested · $swiped swiped",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp),
            )
        }
        SiteWebView(
            controller = controller,
            startUrl = vm.browseUrl,
            onPayload = vm::onPayload,
            onUrlChanged = { vm.browseUrl = it },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
