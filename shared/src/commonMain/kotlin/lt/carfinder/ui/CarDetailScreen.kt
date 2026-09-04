package lt.carfinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.carfinder.AppViewModel
import lt.carfinder.engine.MatchEngine
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
            CarArt(car, Modifier.fillMaxWidth().height(200.dp), emojiSize = 88.sp)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        car.priceEur.asPrice(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    scored?.let { MatchBadge(it.score) }
                }
                Text(
                    car.blurb,
                    style = MaterialTheme.typography.bodyMedium,
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
                    SpecCard("Body", car.bodyType.label())
                    SpecCard("Fuel", car.fuelType.label())
                    SpecCard("Gearbox", car.gearbox.label())
                    SpecCard("Drive", car.drive.label())
                    SpecCard("Seats", "${car.seats}")
                    SpecCard("Power", "${car.powerHp} hp")
                    SpecCard("Boot", "${car.trunkL.groupThousands()} l")
                    SpecCard(
                        "Consumption",
                        "${car.consumption} ${if (car.fuelType == lt.carfinder.model.FuelType.EV) "kWh" else "l"}/100 km",
                    )
                    if (car.rangeKm > 0) SpecCard("Range", "${car.rangeKm} km")
                    SpecCard("Running costs", "~${MatchEngine.annualCostEur(car).groupThousands()} €/yr")
                    SpecCard("Year", "${car.year}")
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
