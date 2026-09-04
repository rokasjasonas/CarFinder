package lt.carfinder.engine

import lt.carfinder.model.BodyType
import lt.carfinder.model.Car
import lt.carfinder.model.Drive
import lt.carfinder.model.FuelType
import lt.carfinder.model.Gearbox
import lt.carfinder.model.Usage
import lt.carfinder.model.UserPrefs
import lt.carfinder.util.groupThousands
import lt.carfinder.util.usageLabel
import kotlin.math.min
import kotlin.random.Random

data class Component(
    val key: String,
    val label: String,
    val value: Float,
    val weight: Float,
    val reason: String? = null,
)

data class ScoredCar(
    val car: Car,
    val score: Int,
    val components: List<Component>,
    val reasons: List<String>,
)

object MatchEngine {

    const val HARD_BUDGET_FACTOR = 1.3f
    const val ANNUAL_KM = 15000
    const val MAX_TASTE_WEIGHT = 3f
    const val LEARNING_RATE = 0.15f

    fun annualCostEur(car: Car): Int {
        val energyPerUnit = if (car.fuelType == FuelType.EV) 0.25f
        else if (car.fuelType == FuelType.DIESEL) 1.55f
        else 1.65f
        val energyYear = (car.consumption * energyPerUnit).toFloat() * ANNUAL_KM / 100f
        return (energyYear + 350f).toInt()
    }

    fun priceBucket(priceEur: Int): String = when {
        priceEur < 15000 -> "under 15k"
        priceEur < 30000 -> "15-30k"
        priceEur < 50000 -> "30-50k"
        else -> "50k+"
    }

    fun excluded(car: Car, prefs: UserPrefs): Boolean =
        car.priceEur > prefs.budgetEur * HARD_BUDGET_FACTOR ||
            car.seats < prefs.minSeats ||
            (prefs.gearbox != null && car.gearbox != prefs.gearbox)

    fun tasteWeight(swipes: Int): Float = MAX_TASTE_WEIGHT * min(1f, swipes / 10f)

    fun score(car: Car, prefs: UserPrefs, affinity: Map<String, Float>, tasteW: Float): ScoredCar {
        val comps = buildList {
            add(budget(car, prefs))
            add(usage(car, prefs))
            add(cost(car, prefs))
            add(space(car, prefs))
            add(performance(car, prefs))
            add(eco(car, prefs))
            add(funFactor(car, prefs))
            add(fuel(car, prefs))
            prefs.gearbox?.let { add(gearbox(car, it)) }
            if (tasteW > 0f) add(taste(car, affinity, tasteW))
        }
        val weighted = comps.sumOf { (it.value * it.weight).toDouble() }
        val weights = comps.sumOf { it.weight.toDouble() }
        val total = if (weights == 0.0) 0.0 else weighted / weights
        val reasons = comps.filter { it.reason != null }
            .sortedByDescending { it.value * it.weight }
            .take(3)
            .map { it.reason!! }
        return ScoredCar(car, (total * 100).toInt(), comps, reasons)
    }

    fun rank(cars: List<Car>, prefs: UserPrefs, affinity: Map<String, Float>, tasteW: Float): List<ScoredCar> =
        cars.filter { !excluded(it, prefs) }
            .map { score(it, prefs, affinity, tasteW) }
            .sortedByDescending { it.score }

    fun deck(
        cars: List<Car>,
        prefs: UserPrefs,
        affinity: Map<String, Float>,
        tasteW: Float,
        seen: Set<String>,
        random: Random = Random(42),
        count: Int = 20,
    ): List<Car> {
        var pool = cars.filter { it.id !in seen && !excluded(it, prefs) }
        val out = mutableListOf<Car>()
        while (out.size < count && pool.isNotEmpty()) {
            val scored = pool.map { score(it, prefs, affinity, tasteW) }
            val pick = if (out.size % 3 == 0) scored.maxBy { it.score } else scored.random(random)
            out += pick.car
            pool = pool.filter { it.id != pick.car.id }
        }
        return out
    }

    fun learn(affinity: Map<String, Float>, car: Car, liked: Boolean, lr: Float = LEARNING_RATE): Map<String, Float> {
        val keys = listOf(
            "body:${car.bodyType}",
            "fuel:${car.fuelType}",
            "price:${priceBucket(car.priceEur)}",
        )
        val out = affinity.toMutableMap()
        for (k in keys) out[k] = ((out[k] ?: 0f) + if (liked) lr else -lr).coerceIn(-1f, 1f)
        return out
    }

    private fun budget(car: Car, prefs: UserPrefs) = Component(
        "budget", "Budget",
        run {
            val b = prefs.budgetEur.toFloat()
            if (car.priceEur <= b) 1f else (1f - (car.priceEur - b) / (0.3f * b)).coerceIn(0f, 1f)
        },
        2f,
        if (car.priceEur <= prefs.budgetEur) "${(prefs.budgetEur - car.priceEur).groupThousands()} € under budget" else null,
    )

    private fun usage(car: Car, prefs: UserPrefs): Component {
        var v = when (prefs.usage) {
            Usage.COMMUTE -> when (car.bodyType) {
                BodyType.HATCHBACK, BodyType.SEDAN -> 1f
                BodyType.WAGON -> 0.8f
                BodyType.SUV -> 0.6f
                BodyType.COUPE, BodyType.CONVERTIBLE -> 0.4f
                BodyType.VAN -> 0.3f
                BodyType.PICKUP -> 0.2f
            }
            Usage.FAMILY -> when (car.bodyType) {
                BodyType.WAGON, BodyType.SUV -> 1f
                BodyType.VAN -> 0.9f
                BodyType.SEDAN -> 0.7f
                BodyType.HATCHBACK -> 0.6f
                BodyType.PICKUP -> 0.4f
                BodyType.COUPE -> 0.1f
                BodyType.CONVERTIBLE -> 0.05f
            }
            Usage.SPORT -> when (car.bodyType) {
                BodyType.COUPE -> 1f
                BodyType.CONVERTIBLE -> 0.95f
                BodyType.SEDAN -> 0.4f
                BodyType.HATCHBACK -> 0.35f
                BodyType.WAGON -> 0.3f
                BodyType.SUV -> 0.2f
                BodyType.PICKUP -> 0.1f
                BodyType.VAN -> 0.05f
            }
            Usage.ADVENTURE -> when (car.bodyType) {
                BodyType.SUV -> 1f
                BodyType.PICKUP -> 0.95f
                BodyType.WAGON -> 0.6f
                BodyType.VAN -> 0.35f
                BodyType.HATCHBACK -> 0.3f
                BodyType.SEDAN -> 0.25f
                BodyType.COUPE, BodyType.CONVERTIBLE -> 0.1f
            }
            Usage.CITY -> when (car.bodyType) {
                BodyType.HATCHBACK -> 1f
                BodyType.SEDAN -> 0.6f
                BodyType.COUPE -> 0.5f
                BodyType.WAGON -> 0.5f
                BodyType.SUV -> 0.4f
                BodyType.CONVERTIBLE -> 0.3f
                BodyType.VAN -> 0.2f
                BodyType.PICKUP -> 0.1f
            }
        }
        if ((prefs.usage == Usage.CITY || prefs.usage == Usage.COMMUTE) && car.fuelType == FuelType.EV) v = (v + 0.1f).coerceAtMost(1f)
        if (prefs.usage == Usage.ADVENTURE && car.drive == Drive.AWD) v = (v + 0.15f).coerceAtMost(1f)
        if (prefs.usage == Usage.FAMILY && car.seats >= 6) v = (v + 0.1f).coerceAtMost(1f)
        val reason = if (v >= 0.75f) "Great fit for ${prefs.usage.usageLabel()}" else null
        return Component("usage", "Lifestyle fit", v, 2.5f, reason)
    }

    private fun cost(car: Car, prefs: UserPrefs): Component {
        val annual = annualCostEur(car)
        val v = (1f - (annual - 600f) / 2200f).coerceIn(0f, 1f)
        val reason = if (annual <= 1100 && prefs.weights.runningCost >= 0.3f) "Low running costs (~${annual.groupThousands()} €/yr)" else null
        return Component("cost", "Running costs", v, 1f + 2f * prefs.weights.runningCost, reason)
    }

    private fun space(car: Car, prefs: UserPrefs): Component {
        val seatScore = min(car.seats, prefs.minSeats).toFloat() / prefs.minSeats
        val trunkNeed = when (prefs.usage) {
            Usage.FAMILY -> 500f
            Usage.ADVENTURE -> 450f
            Usage.COMMUTE -> 300f
            Usage.CITY -> 250f
            Usage.SPORT -> 200f
        }
        val trunkScore = (car.trunkL / trunkNeed).coerceAtMost(1f)
        val v = seatScore * 0.6f + trunkScore * 0.4f
        val reason = if (v >= 0.8f) "Fits ${prefs.minSeats} people + luggage" else null
        return Component("space", "Space", v, 1f + 2f * prefs.weights.space, reason)
    }

    private fun performance(car: Car, prefs: UserPrefs): Component {
        val v = ((car.powerHp - 60f) / 290f).coerceIn(0f, 1f)
        val reason = if (v >= 0.6f) "Quick: ${car.powerHp} hp" else null
        return Component("performance", "Performance", v, 1f + 2f * prefs.weights.performance, reason)
    }

    private fun eco(car: Car, prefs: UserPrefs): Component {
        val v = when (car.fuelType) {
            FuelType.EV -> 1f
            FuelType.HYBRID -> 0.75f
            FuelType.PETROL -> 0.45f
            FuelType.DIESEL -> 0.35f
        }
        val reason = if (car.fuelType == FuelType.EV && prefs.weights.eco >= 0.3f) "Zero tailpipe emissions" else null
        return Component("eco", "Eco", v, 1f + 2f * prefs.weights.eco, reason)
    }

    private fun funFactor(car: Car, prefs: UserPrefs): Component {
        val hpNorm = ((car.powerHp - 60f) / 290f).coerceIn(0f, 1f)
        var v = 0.45f + 0.25f * hpNorm
        if (car.drive == Drive.RWD) v += 0.2f else if (car.drive == Drive.AWD) v += 0.1f
        if (car.bodyType == BodyType.COUPE || car.bodyType == BodyType.CONVERTIBLE) v += 0.15f
        v = v.coerceIn(0f, 1f)
        val reason = if (v >= 0.8f) "Proper driver's car" else null
        return Component("fun", "Driving fun", v, 1f + 2f * prefs.weights.drivingFun, reason)
    }

    private fun fuel(car: Car, prefs: UserPrefs): Component {
        val v = if (prefs.fuelPrefs.isEmpty()) 0.5f else if (car.fuelType in prefs.fuelPrefs) 1f else 0.2f
        val reason = if (prefs.fuelPrefs.isNotEmpty() && car.fuelType in prefs.fuelPrefs) "Matches your fuel preference" else null
        return Component("fuel", "Fuel choice", v, if (prefs.fuelPrefs.isEmpty()) 0f else 1.5f, reason)
    }

    private fun gearbox(car: Car, pref: Gearbox): Component {
        val v = if (car.gearbox == pref) 1f else 0f
        val reason = if (v == 1f && pref == Gearbox.AUTOMATIC) "Automatic gearbox" else null
        return Component("gearbox", "Gearbox", v, 1f, reason)
    }

    private fun taste(car: Car, affinity: Map<String, Float>, weight: Float): Component {
        val keys = listOf(
            "body:${car.bodyType}",
            "fuel:${car.fuelType}",
            "price:${priceBucket(car.priceEur)}",
        )
        val v = keys.map { ((affinity[it] ?: 0f) + 1f) / 2f }.average().toFloat()
        val reason = if (v >= 0.65f) "Similar to cars you liked" else null
        return Component("taste", "Your taste", v, weight, reason)
    }
}
