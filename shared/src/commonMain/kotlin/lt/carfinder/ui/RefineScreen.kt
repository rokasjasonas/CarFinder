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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.carfinder.AppViewModel
import lt.carfinder.model.BodyType
import lt.carfinder.util.asPrice
import lt.carfinder.util.groupThousands
import lt.carfinder.util.label
import lt.carfinder.util.usageLabel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RefineScreen(vm: AppViewModel) {
    val state = vm.state
    val prefs = state.prefs ?: return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Refine", style = MaterialTheme.typography.headlineSmall)
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Match precision", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${vm.sharpness}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(progress = { vm.sharpness / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                Text(
                    "Every answer sharpens every match. ${5 - RefineExtra.answeredExtra(state.answered)} of 5 questions left.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        QuestionCard("Oldest year you'd accept", "year", state.answered) {
            YearSlider(prefs.minYear ?: 2012) { v -> vm.answerRefine("year") { it.copy(minYear = v) } }
        }
        QuestionCard("Max mileage you'd tolerate", "mileage", state.answered) {
            MileageSlider(prefs.maxMileageKm ?: 150_000) { v -> vm.answerRefine("mileage") { it.copy(maxMileageKm = v) } }
        }
        QuestionCard("Minimum power you'd enjoy", "power", state.answered) {
            PowerSlider(prefs.minPowerHp ?: 110) { v -> vm.answerRefine("power") { it.copy(minPowerHp = v) } }
        }
        QuestionCard("Body styles you like", "bodies", state.answered) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyType.entries.forEach { b ->
                    FilterChip(
                        selected = b in prefs.likedBodies,
                        onClick = {
                            vm.answerRefine("bodies") { p ->
                                p.copy(likedBodies = if (b in p.likedBodies) p.likedBodies - b else p.likedBodies + b)
                            }
                        },
                        label = { Text(b.label()) },
                    )
                }
            }
        }
        QuestionCard("Brands you especially like", "brands", state.answered) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.topBrands().forEach { brand ->
                    FilterChip(
                        selected = brand in prefs.likedBrands,
                        onClick = {
                            vm.answerRefine("brands") { p ->
                                p.copy(likedBrands = if (brand in p.likedBrands) p.likedBrands - brand else p.likedBrands + brand)
                            }
                        },
                        label = { Text(brand) },
                    )
                }
            }
            if (vm.topBrands().isEmpty()) {
                Text(
                    "Appears once I've fetched the first listings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your answers so far", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                PrefRow("Budget", prefs.budgetEur.asPrice())
                PrefRow("Main use", prefs.usage.usageLabel().replaceFirstChar { it.uppercase() })
                PrefRow("Gearbox", prefs.gearbox?.label() ?: "Any")
                PrefRow("Fuel", if (prefs.fuelPrefs.isEmpty()) "Any" else prefs.fuelPrefs.joinToString { it.label() })
                prefs.minYear?.let { PrefRow("Oldest year", "$it+") }
                prefs.maxMileageKm?.let { PrefRow("Max mileage", "${it.groupThousands()} km") }
                prefs.minPowerHp?.let { PrefRow("Min power", "$it hp") }
                if (prefs.likedBodies.isNotEmpty()) PrefRow("Liked bodies", prefs.likedBodies.joinToString { it.label() })
                if (prefs.likedBrands.isNotEmpty()) PrefRow("Liked brands", prefs.likedBrands.joinToString())
                Spacer(Modifier.height(4.dp))
                WeightBar("Running costs", prefs.weights.runningCost)
                WeightBar("Space", prefs.weights.space)
                WeightBar("Performance", prefs.weights.performance)
                WeightBar("Eco", prefs.weights.eco)
                WeightBar("Driving fun", prefs.weights.drivingFun)
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Taste learning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Fetched ${state.listings.size} listings from autoplius.lt & autogidas.lt · liked ${state.likedCount} · passed ${state.passedCount}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    if (state.swipes.size < 10) {
                        "Keep swiping — I'm still learning your taste (${state.swipes.size}/10 swipes)."
                    } else {
                        "Taste model fully warmed up: matches now lean on what you actually liked."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(onClick = { vm.retakeQuiz() }, modifier = Modifier.fillMaxWidth()) { Text("Retake the quiz") }
        OutlinedButton(onClick = { vm.resetTaste() }, modifier = Modifier.fillMaxWidth()) { Text("Reset taste learning") }
        Spacer(Modifier.height(8.dp))
    }
}

private object RefineExtra {
    val ids = listOf("year", "mileage", "bodies", "brands", "power")
    fun answeredExtra(answered: Set<String>): Int = ids.count { it in answered }
}

@Composable
private fun QuestionCard(title: String, id: String, answered: Set<String>, content: @Composable () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (id !in answered) AssistChip(onClick = {}, label = { Text("new") })
            }
            content()
        }
    }
}

@Composable
private fun YearSlider(current: Int, onCommit: (Int) -> Unit) {
    var pending by remember { mutableStateOf<Float?>(null) }
    val v = pending ?: current.toFloat()
    Text("From ${v.roundToInt()} onwards", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = v,
        onValueChange = { pending = it },
        onValueChangeFinished = {
            onCommit((v).roundToInt())
            pending = null
        },
        valueRange = 1998f..2025f,
    )
}

@Composable
private fun MileageSlider(current: Int, onCommit: (Int) -> Unit) {
    var pending by remember { mutableStateOf<Float?>(null) }
    val v = pending ?: current.toFloat()
    Text("Up to ${v.roundToInt().groupThousands()} km", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = v,
        onValueChange = { pending = it },
        onValueChangeFinished = {
            onCommit((v.roundToInt() / 5_000) * 5_000)
            pending = null
        },
        valueRange = 20_000f..300_000f,
        steps = 55,
    )
}

@Composable
private fun PowerSlider(current: Int, onCommit: (Int) -> Unit) {
    var pending by remember { mutableStateOf<Float?>(null) }
    val v = pending ?: current.toFloat()
    Text("At least ${v.roundToInt()} hp", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = v,
        onValueChange = { pending = it },
        onValueChangeFinished = {
            onCommit((v.roundToInt() / 10) * 10)
            pending = null
        },
        valueRange = 60f..300f,
        steps = 23,
    )
}

@Composable
private fun PrefRow(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun WeightBar(label: String, weight: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        LinearProgressIndicator(
            progress = { weight },
            modifier = Modifier.width(120.dp).height(6.dp),
        )
    }
}
