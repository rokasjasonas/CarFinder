package lt.carfinder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbsUpDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import lt.carfinder.AppViewModel
import lt.carfinder.Route
import lt.carfinder.Tab

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(vm: AppViewModel = viewModel { AppViewModel() }) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF9BB1FF),
            background = Color(0xFF101319),
            surface = Color(0xFF161B24),
        )
    } else {
        lightColorScheme(primary = Color(0xFF3B5BDB))
    }
    MaterialTheme(colorScheme = scheme) {
        if (!vm.hasPrefs) {
            QuizScreen(vm)
            return@MaterialTheme
        }
        val top = vm.stack.lastOrNull()
        BackHandler(enabled = top != null) { vm.back() }
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Tabs(vm)
                if (top != null) {
                    Surface(Modifier.fillMaxSize()) {
                        when (top) {
                            is Route.CarDetail -> CarDetailScreen(vm, top.carId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tabs(vm: AppViewModel) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = vm.tab == Tab.Browse,
                    onClick = { vm.tab = Tab.Browse },
                    icon = { Icon(Icons.Default.Search, null) },
                    label = { Text("Browse") },
                )
                NavigationBarItem(
                    selected = vm.tab == Tab.Discover,
                    onClick = { vm.tab = Tab.Discover },
                    icon = { Icon(Icons.Default.ThumbsUpDown, null) },
                    label = { Text("Discover") },
                )
                NavigationBarItem(
                    selected = vm.tab == Tab.Matches,
                    onClick = { vm.tab = Tab.Matches },
                    icon = { Icon(Icons.Default.Favorite, null) },
                    label = { Text("Matches") },
                )
                NavigationBarItem(
                    selected = vm.tab == Tab.Profile,
                    onClick = { vm.tab = Tab.Profile },
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Profile") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // The WebView must stay mounted across tab switches or it reloads and loses history.
            BrowseScreen(vm, visible = vm.tab == Tab.Browse)
            if (vm.tab != Tab.Browse) {
                Surface(Modifier.fillMaxSize()) {
                    when (vm.tab) {
                        Tab.Discover -> SwipeScreen(vm)
                        Tab.Matches -> MatchesScreen(vm)
                        Tab.Profile -> ProfileScreen(vm)
                        Tab.Browse -> Unit
                    }
                }
            }
        }
    }
}
