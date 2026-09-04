package lt.carfinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.carfinder.model.BodyType
import lt.carfinder.model.Car
import lt.carfinder.model.FuelType
import lt.carfinder.model.Source
import lt.carfinder.util.label
import kotlin.math.absoluteValue

fun Color.darker(f: Float): Color = Color(red * f, green * f, blue * f, alpha)

fun scoreColor(score: Int): Color = when {
    score >= 85 -> Color(0xFF2E7D32)
    score >= 70 -> Color(0xFF3B5BDB)
    else -> Color(0xFFB26A00)
}

private val palette = listOf(
    0xFF3B5BDB, 0xFF2A6041, 0xFF35495E, 0xFF6B4F3A, 0xFF8B1E3F, 0xFF31572C,
    0xFF1B4965, 0xFF52796F, 0xFF4E6E8E, 0xFF264653, 0xFFB08968, 0xFF2C3E50,
)

private fun Car.accentColor(): Color = Color(palette[id.hashCode().absoluteValue % palette.size])

private fun Car.carEmoji(): String = when (bodyType) {
    BodyType.SUV -> "🚙"
    BodyType.COUPE, BodyType.CONVERTIBLE -> "🏎️"
    BodyType.PICKUP -> "🛻"
    BodyType.VAN -> "🚐"
    BodyType.WAGON -> "🚘"
    else -> "🚗"
}

private fun Car.brandName(): String = title.split(" ").firstOrNull() ?: title

/** Procedural card art for cars without photos. */
@Composable
fun CarArt(car: Car, modifier: Modifier = Modifier, emojiSize: androidx.compose.ui.unit.TextUnit = 72.sp) {
    val base = car.accentColor()
    Box(modifier.background(Brush.verticalGradient(listOf(base, base.darker(0.55f))))) {
        Text(
            car.carEmoji(),
            modifier = Modifier.align(Alignment.Center).alpha(0.95f),
            fontSize = emojiSize,
        )
        Text(
            car.brandName(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.10f),
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        )
    }
}

@Composable
fun MatchBadge(score: Int, modifier: Modifier = Modifier) {
    Surface(color = scoreColor(score), contentColor = Color.White, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            "$score% match",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun Stamp(label: String, color: Color, strength: Float, modifier: Modifier) {
    Text(
        label,
        color = color,
        fontWeight = FontWeight.Black,
        style = MaterialTheme.typography.headlineSmall,
        modifier = modifier
            .padding(20.dp)
            .alpha(strength)
            .rotate(if (label == "LIKE") -12f else 12f)
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

fun Source.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

fun Car.fuelLabel(): String = fuelType?.label() ?: "—"

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
