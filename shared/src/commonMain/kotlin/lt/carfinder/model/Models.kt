package lt.carfinder.model

import kotlinx.serialization.Serializable

enum class BodyType { HATCHBACK, SEDAN, WAGON, SUV, COUPE, CONVERTIBLE, PICKUP, VAN }
enum class FuelType { PETROL, DIESEL, HYBRID, EV }
enum class Gearbox { MANUAL, AUTOMATIC }
enum class Drive { FWD, RWD, AWD }
enum class Usage { COMMUTE, FAMILY, SPORT, ADVENTURE, CITY }
enum class Source { AUTOPLIUS, AUTOGIDAS }

@Serializable
data class Car(
    val id: String,
    val source: Source,
    val url: String,
    val title: String,
    val priceEur: Int? = null,
    val year: Int? = null,
    val mileageKm: Int? = null,
    val fuelType: FuelType? = null,
    val gearbox: Gearbox? = null,
    val bodyType: BodyType? = null,
    val powerHp: Int? = null,
    val engine: String? = null,
    val photos: List<String> = emptyList(),
    val capturedAt: Long = 0,
)

@Serializable
data class Weights(
    val runningCost: Float = 0.4f,
    val space: Float = 0.4f,
    val performance: Float = 0.4f,
    val eco: Float = 0.4f,
    val drivingFun: Float = 0.4f,
)

@Serializable
data class UserPrefs(
    val budgetEur: Int,
    val usage: Usage,
    val gearbox: Gearbox? = null,
    val fuelPrefs: Set<FuelType> = emptySet(),
    val weights: Weights = Weights(),
)

@Serializable
data class Swipe(val carId: String, val liked: Boolean)

@Serializable
data class AppState(
    val prefs: UserPrefs? = null,
    val listings: List<Car> = emptyList(),
    val swipes: List<Swipe> = emptyList(),
    val affinity: Map<String, Float> = emptyMap(),
) {
    val likedCount: Int get() = swipes.count { it.liked }
    val passedCount: Int get() = swipes.count { !it.liked }
}
