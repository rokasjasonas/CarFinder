package lt.carfinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import lt.carfinder.AppViewModel
import lt.carfinder.Route
import lt.carfinder.Tab
import lt.carfinder.engine.Ask
import lt.carfinder.engine.BestMatch
import lt.carfinder.engine.MatchEngine
import lt.carfinder.engine.Refine
import lt.carfinder.platform.openInBrowser
import lt.carfinder.util.asPrice
import lt.carfinder.util.groupThousands
import lt.carfinder.util.label

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MatchCarScreen(vm: AppViewModel) {
    val state = vm.state
    val best = remember(state) { vm.bestMatch() }
    var askDismissed by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your match car") },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.listings.isEmpty() -> Centered {
                    CircularProgressIndicator(Modifier.size(48.dp))
                    Text(
                        "Fetching listings for you…",
                        Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                best == null -> Centered {
                    Text("No car survives your filters", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Loosen a filter or raise the budget, then try again.",
                        Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { vm.back(); vm.tab = Tab.Refine },
                        Modifier.padding(top = 16.dp),
                    ) { Text("Adjust answers") }
                }
                else -> MatchCarContent(vm, best, askDismissed, onDismissAsk = { askDismissed = true })
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun MatchCarContent(vm: AppViewModel, best: BestMatch, askDismissed: Boolean, onDismissAsk: () -> Unit) {
    val car = best.top.car
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(220.dp)) {
            if (car.photos.isNotEmpty()) {
                AsyncImage(
                    model = car.photos.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CarArt(car, Modifier.fillMaxSize(), emojiSize = 96.sp)
            }
            MatchBadge(best.top.score, Modifier.align(Alignment.TopStart).padding(12.dp))
            Text(
                car.source.label(),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(
                    car.priceEur?.asPrice() ?: "Price on request",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    car.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ConfidenceCard(vm, best)
            best.ask?.takeIf { !askDismissed }?.let { AskCard(vm, it, onDismissAsk) }
            if (best.suggestions.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Want it sharper? Answer more in Refine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { vm.back(); vm.tab = Tab.Refine }) { Text("Refine") }
                }
            }
            best.runnerUp?.let { ru ->
                Card(Modifier.fillMaxWidth().clickable { vm.open(Route.CarDetail(ru.car.id)) }) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text("Next best", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        // After the documentation tiebreak the runner-up can out-score the pick;
                        // a higher % here reads as a bug, so show why it lost instead.
                        val detail = if (ru.score > best.top.score) {
                            if (ru.car.priceEur == null) "no price listed — can't check against your budget" else "listing is missing key details"
                        } else " · ${ru.score}%"
                        Text(
                            "${ru.car.title} $detail",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            if (best.top.reasons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Why this one", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    best.top.reasons.forEach { r -> Text("✓ $r", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (car.priceEur != null) SpecCard("Price", car.priceEur.asPrice())
                if (car.year != null) SpecCard("Year", "${car.year}")
                if (car.mileageKm != null) SpecCard("Mileage", "${car.mileageKm.groupThousands()} km")
                if (car.bodyType != null) SpecCard("Size / body", car.bodyType.label())
                if (car.fuelType != null) SpecCard("Fuel", car.fuelType.label())
                if (car.powerHp != null) SpecCard("Power", "${car.powerHp} hp")
                if (car.engine != null) SpecCard("Engine", car.engine)
                if (car.gearbox != null) SpecCard("Gearbox", car.gearbox.label())
                MatchEngine.annualCostEur(car)?.let { SpecCard("Running costs est.", "~${it.groupThousands()} €/yr") }
            }
            Button(onClick = { openInBrowser(car.url) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View on ${car.source.label()}")
            }
        }
    }
}

@Composable
private fun ConfidenceCard(vm: AppViewModel, best: BestMatch) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                " ${best.confidence}% sure this fits you",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }
        LinearProgressIndicator(
            progress = { best.confidence / 100f },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp),
        )
        val swipes = vm.state.swipes.size
        val answered = (Refine.INITIAL + Refine.EXTRA).count { it in vm.state.answered }
        Text(
            "Learned from $swipes swipe${if (swipes == 1) "" else "s"} and $answered/10 answers" +
                (if (swipes < 10 || answered < 10) " — it sharpens as you go" else "") + ".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskCard(vm: AppViewModel, ask: Ask, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(
            "One quick question",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(ask.question, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ask.options.forEach { opt ->
                AssistChip(onClick = { vm.answerAsk(ask.id, opt.patch) }, label = { Text(opt.label) })
            }
            AssistChip(onClick = onDismiss, label = { Text("Skip") })
        }
    }
}
