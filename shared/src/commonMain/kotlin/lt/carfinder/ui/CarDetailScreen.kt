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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import lt.carfinder.engine.MatchEngine
import lt.carfinder.platform.openInBrowser
import lt.carfinder.util.asPrice
import lt.carfinder.util.groupThousands
import lt.carfinder.util.label
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CarDetailScreen(vm: AppViewModel, carId: String) {
    val car = vm.car(carId) ?: return
    val scored = vm.scored(car)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(car.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                if (car.photos.isNotEmpty()) {
                    var photo by remember(car.id) { mutableStateOf(0) }
                    if (photo >= car.photos.size) photo = 0
                    AsyncImage(
                        model = car.photos.getOrNull(photo),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (car.photos.size > 1) {
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxSize().clickable { if (photo > 0) photo-- else photo = car.photos.size - 1 })
                            Box(Modifier.weight(1f).fillMaxSize().clickable { photo = (photo + 1) % car.photos.size })
                        }
                    }
                } else {
                    CarArt(car, Modifier.fillMaxSize(), emojiSize = 96.sp)
                }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        car.priceEur?.asPrice() ?: "Price on request",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    scored?.let { MatchBadge(it.score) }
                }
                Text(
                    car.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                scored?.let { sc ->
                    if (sc.reasons.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Why we recommend it", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            sc.reasons.forEach { r -> Text("✓ $r", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Score breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        sc.components.forEach { c ->
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(
                                        "${(c.value * 100).roundToInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { c.value },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp),
                                )
                            }
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (car.year != null) SpecCard("Year", "${car.year}")
                    if (car.mileageKm != null) SpecCard("Mileage", "${car.mileageKm.groupThousands()} km")
                    if (car.fuelType != null) SpecCard("Fuel", car.fuelType.label())
                    if (car.gearbox != null) SpecCard("Gearbox", car.gearbox.label())
                    if (car.bodyType != null) SpecCard("Body", car.bodyType.label())
                    if (car.powerHp != null) SpecCard("Power", "${car.powerHp} hp")
                    if (car.engine != null) SpecCard("Engine", car.engine)
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
}

@Composable
private fun SpecCard(label: String, value: String) {
    Card {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
