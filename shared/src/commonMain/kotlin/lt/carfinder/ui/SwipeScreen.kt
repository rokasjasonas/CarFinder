package lt.carfinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.carfinder.AppViewModel
import lt.carfinder.Route
import lt.carfinder.Tab
import lt.carfinder.model.Car
import lt.carfinder.util.asPrice
import lt.carfinder.util.label
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeScreen(vm: AppViewModel) {
    val state = vm.state
    val deck = remember(state) { vm.deck() }
    val top = deck.firstOrNull()
    val next = deck.getOrNull(1)
    var offsetX by remember(top?.id) { mutableStateOf(0f) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Find your perfect car", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Text("${deck.size} left", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                "Swipe right if you love it · left to pass",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (top == null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("You've seen every car that fits", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Check your ranked matches or adjust your preferences to widen the search.",
                    Modifier.padding(top = 8.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { vm.tab = Tab.Matches }) { Text("See your matches") }
                OutlinedButton(onClick = { vm.tab = Tab.Profile }) { Text("Adjust preferences") }
            }
            return@Column
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            val threshold = constraints.maxWidth * 0.3f
            val progress = (offsetX / threshold).coerceIn(-1f, 1f)
            next?.let {
                CarCard(vm, it, Modifier.fillMaxSize().scale(0.95f + 0.05f * abs(progress)).alpha(0.6f + 0.4f * abs(progress)))
            }
            Box(
                Modifier.fillMaxSize()
                    .offset { IntOffset(offsetX.roundToInt(), (abs(offsetX) * 0.08f).roundToInt()) }
                    .rotate(progress * 8f)
                    .pointerInput(top.id) {
                        detectDragGestures(
                            onDragEnd = {
                if (abs(offsetX) > threshold) vm.swipe(top.id, liked = offsetX > 0) else offsetX = 0f
                            },
                            onDragCancel = { offsetX = 0f },
                        ) { change, drag -> change.consume(); offsetX += drag.x }
                    },
            ) {
                CarCard(vm, top, Modifier.fillMaxSize())
                if (progress > 0.15f) Stamp("LIKE", Color(0xFF2E7D32), progress, Modifier.align(Alignment.TopStart))
                if (progress < -0.15f) Stamp("NOPE", Color(0xFFC62828), -progress, Modifier.align(Alignment.TopEnd))
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = { vm.swipe(top.id, false) },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFFFDECEA), contentColor = Color(0xFFC62828)),
            ) { Icon(Icons.Default.Close, "Pass") }
            FilledTonalIconButton(
                onClick = { vm.open(Route.CarDetail(top.id)) },
                modifier = Modifier.size(48.dp),
            ) { Icon(Icons.Default.Info, "Details") }
            FilledTonalIconButton(
                onClick = { vm.swipe(top.id, true) },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFFE6F4EA), contentColor = Color(0xFF2E7D32)),
            ) { Icon(Icons.Default.Favorite, "Like") }
        }
    }
}

@Composable
private fun CarCard(vm: AppViewModel, car: Car, modifier: Modifier) {
    val scored = vm.scored(car)
    Card(modifier, shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(6.dp)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))) {
                CarArt(car, Modifier.fillMaxSize(), emojiSize = 84.sp)
                scored?.let {
                    MatchBadge(it.score, Modifier.align(Alignment.TopStart).padding(12.dp))
                }
                Text(
                    car.year.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Column(
                Modifier.fillMaxWidth().clickable { vm.open(Route.CarDetail(car.id)) }.padding(16.dp),
            ) {
                Text(car.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    car.priceEur.asPrice(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                Text(
                    listOf(
                        car.bodyType.label(),
                        car.fuelType.label(),
                        car.gearbox.label(),
                        "${car.powerHp} hp",
                        "${car.seats} seats",
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                scored?.reasons?.take(2)?.let { reasons ->
                    if (reasons.isNotEmpty()) {
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            reasons.forEach { r -> ReasonChip(r, Modifier.weight(1f, fill = false)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReasonChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
