package lt.carfinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.carfinder.AppViewModel
import lt.carfinder.util.asPrice
import lt.carfinder.util.label
import lt.carfinder.util.usageLabel

@Composable
fun ProfileScreen(vm: AppViewModel) {
    val state = vm.state
    val prefs = state.prefs ?: return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Your preferences", style = MaterialTheme.typography.headlineSmall)
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrefRow("Budget", prefs.budgetEur.asPrice())
                PrefRow("Main use", prefs.usage.usageLabel().replaceFirstChar { it.uppercase() })
                PrefRow("Gearbox", prefs.gearbox?.label() ?: "Any")
                PrefRow("Fuel", if (prefs.fuelPrefs.isEmpty()) "Any" else prefs.fuelPrefs.joinToString { it.label() })
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
                    "Harvested ${state.listings.size} listings from autoplius.lt & autogidas.lt · liked ${state.likedCount} · passed ${state.passedCount}",
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
    }
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
