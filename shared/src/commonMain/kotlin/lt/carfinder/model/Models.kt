package lt.carfinder.model

import kotlinx.serialization.Serializable

enum class BodyType { HATCHBACK, SEDAN, WAGON, SUV, COUPE, CONVERTIBLE, PICKUP, VAN }
enum class FuelType { PETROL, DIESEL, HYBRID, EV }
enum class Gearbox { MANUAL, AUTOMATIC }
enum class Drive { FWD, RWD, AWD }
enum class Usage { COMMUTE, FAMILY, SPORT, ADVENTURE, CITY }

@Serializable
data class Car(
    val id: String,
    val brand: String,
    val model: String,
    val year: Int,
    val priceEur: Int,
    val bodyType: BodyType,
    val fuelType: FuelType,
    val gearbox: Gearbox,
    val seats: Int,
    val powerHp: Int,
    val trunkL: Int,
    val consumption: Double,
    val drive: Drive = Drive.FWD,
    val rangeKm: Int = 0,
    val blurb: String = "",
    val emoji: String,
    val accent: Long,
) {
    val title: String get() = "$brand $model"
}

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
    val minSeats: Int = 4,
    val gearbox: Gearbox? = null,
    val fuelPrefs: Set<FuelType> = emptySet(),
    val weights: Weights = Weights(),
)

@Serializable
data class Swipe(val carId: String, val liked: Boolean)

@Serializable
data class AppState(
    val prefs: UserPrefs? = null,
    val swipes: List<Swipe> = emptyList(),
    val affinity: Map<String, Float> = emptyMap(),
) {
    val likedCount: Int get() = swipes.count { it.liked }
    val passedCount: Int get() = swipes.count { !it.liked }
}
