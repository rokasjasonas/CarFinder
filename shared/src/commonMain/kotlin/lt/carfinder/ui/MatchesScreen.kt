package lt.carfinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import lt.carfinder.AppViewModel
import lt.carfinder.Route
import lt.carfinder.engine.ScoredCar
import lt.carfinder.util.asPrice
import lt.carfinder.util.label

@Composable
fun MatchesScreen(vm: AppViewModel) {
    val state = vm.state
    val matches = remember(state) { vm.matches() }
    val likedIds = state.swipes.filter { it.liked }.map { it.carId }.toSet()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("Your top matches", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Ranked by your quiz answers + the taste learned from your swipes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(matches.take(12), key = { it.car.id }) { sc ->
            MatchRow(sc, liked = sc.car.id in likedIds) { vm.open(Route.CarDetail(sc.car.id)) }
        }
        if (matches.size > 12) {
            item {
                Text(
                    "and ${matches.size - 12} more…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MatchRow(sc: ScoredCar, liked: Boolean, onClick: () -> Unit) {
    val car = sc.car
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box {
                if (car.photos.isNotEmpty()) {
                    AsyncImage(
                        model = car.photos.first(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(width = 110.dp, height = 84.dp).clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    CarArt(car, Modifier.size(width = 110.dp, height = 84.dp).clip(RoundedCornerShape(12.dp)), emojiSize = 36.sp)
                }
                MatchBadge(sc.score, Modifier.align(Alignment.TopStart).padding(6.dp))
                if (liked) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Liked",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(18.dp),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    car.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        car.priceEur?.asPrice(),
                        car.year?.toString(),
                        car.fuelType?.label(),
                        car.powerHp?.let { "$it hp" },
                    ).joinToString("  ·  ").ifEmpty { car.source.name.lowercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                sc.reasons.take(2).forEach { r ->
                    Text(
                        "✓ $r",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
