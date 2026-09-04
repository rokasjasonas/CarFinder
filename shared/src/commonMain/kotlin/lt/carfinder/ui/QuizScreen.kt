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
import androidx.compose.material3.Button
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
import lt.carfinder.model.FuelType
import lt.carfinder.model.Gearbox
import lt.carfinder.model.Usage
import lt.carfinder.model.UserPrefs
import lt.carfinder.model.Weights
import lt.carfinder.util.asPrice
import lt.carfinder.util.label
import lt.carfinder.util.shortLabel
import lt.carfinder.util.usageLabel
import kotlin.math.roundToInt

private val PRIORITIES = listOf(
    "cost" to "💶 Running costs",
    "space" to "📦 Space",
    "performance" to "⚡ Performance",
    "eco" to "🌱 Eco",
    "fun" to "🛣️ Driving fun",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizScreen(vm: AppViewModel) {
    var step by remember { mutableStateOf(0) }
    var budget by remember { mutableStateOf(25000) }
    var usage by remember { mutableStateOf<Usage?>(null) }
    var minSeats by remember { mutableStateOf(5) }
    var fuels by remember { mutableStateOf(emptySet<FuelType>()) }
    var gearbox by remember { mutableStateOf<Gearbox?>(null) }
    var priority by remember { mutableStateOf<String?>(null) }

    val stepValid = when (step) {
        0 -> true
        1 -> usage != null
        else -> true
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        LinearProgressIndicator(progress = { (step + 1) / 6f }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
        when (step) {
            0 -> {
                QuestionTitle("What's your budget?")
                Text(
                    budget.asPrice(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 24.dp),
                )
                Slider(
                    value = budget.toFloat(),
                    onValueChange = { budget = ((it.roundToInt() / 500) * 500) },
                    valueRange = 5000f..100000f,
                )
                Text(
                    "Cars up to 30% over this can still appear, but are penalised.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            1 -> {
                QuestionTitle("What will you use it for most?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Usage.entries.forEach { u ->
                        FilterChip(
                            selected = usage == u,
                            onClick = { usage = u },
                            label = { Text(u.shortLabel()) },
                        )
                    }
                }
                usage?.let {
                    Text(
                        "Got it — optimising for ${it.usageLabel()}.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            2 -> {
                QuestionTitle("How many seats do you need?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 4, 5, 7).forEach { n ->
                        FilterChip(
                            selected = minSeats == n,
                            onClick = { minSeats = n },
                            label = { Text(if (n == 7) "7+" else "$n") },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                QuestionTitle("Preferred fuel?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = fuels.isEmpty(),
                        onClick = { fuels = emptySet() },
                        label = { Text("Any") },
                    )
                    FuelType.entries.forEach { f ->
                        FilterChip(
                            selected = f in fuels,
                            onClick = { fuels = if (f in fuels) fuels - f else fuels + f },
                            label = { Text(f.label()) },
                        )
                    }
                }
            }
            3 -> {
                QuestionTitle("Gearbox preference?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = gearbox == null, onClick = { gearbox = null }, label = { Text("No preference") })
                    FilterChip(selected = gearbox == Gearbox.AUTOMATIC, onClick = { gearbox = Gearbox.AUTOMATIC }, label = { Text("Automatic") })
                    FilterChip(selected = gearbox == Gearbox.MANUAL, onClick = { gearbox = Gearbox.MANUAL }, label = { Text("Manual") })
                }
            }
            4 -> {
                QuestionTitle("What matters most to you?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRIORITIES.forEach { (key, label) ->
                        FilterChip(
                            selected = priority == key,
                            onClick = { priority = key },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    "Your top pick gets full weight; the others still count, just less.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            else -> {
                QuestionTitle("All set!")
                Text(
                    "I'll look for a ${usage?.usageLabel() ?: "versatile"} car for ${budget.asPrice()}, " +
                        "$minSeats+ seats, " +
                        when {
                            gearbox == Gearbox.AUTOMATIC -> "automatic gearbox"
                            gearbox == Gearbox.MANUAL -> "manual gearbox"
                            else -> "any gearbox"
                        } +
                        if (fuels.isEmpty()) "" else ", running on ${fuels.joinToString(" or ") { it.label().lowercase() }}.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "Every swipe teaches me your taste — matches update as you go.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) {
                OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Back") }
            }
            Button(
                onClick = {
                    if (step < 5) step++
                    else {
                        val w = Weights(
                            runningCost = if (priority == "cost") 1f else 0.35f,
                            space = if (priority == "space") 1f else 0.35f,
                            performance = if (priority == "performance") 1f else 0.35f,
                            eco = if (priority == "eco") 1f else 0.35f,
                            drivingFun = if (priority == "fun") 1f else 0.35f,
                        )
                        vm.completeQuiz(
                            UserPrefs(
                                budgetEur = budget,
                                usage = usage ?: Usage.COMMUTE,
                                minSeats = minSeats,
                                gearbox = gearbox,
                                fuelPrefs = fuels,
                                weights = w,
                            ),
                        )
                    }
                },
                enabled = stepValid,
                modifier = Modifier.weight(1f),
            ) { Text(if (step < 5) "Next" else "Start swiping") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuestionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}
