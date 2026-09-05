package lt.carfinder.engine

import lt.carfinder.model.BodyType
import lt.carfinder.model.Car
import lt.carfinder.model.Drive
import lt.carfinder.model.FuelType
import lt.carfinder.model.Gearbox
import lt.carfinder.model.Usage
import lt.carfinder.model.UserPrefs
import lt.carfinder.util.groupThousands
import lt.carfinder.util.label
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

/** One targeted follow-up question with ready-made answer chips. */
data class Ask(
    val id: String,
    val question: String,
    val options: List<AskOption>,
)

data class AskOption(
    val label: String,
    val patch: (UserPrefs) -> UserPrefs,
)

/** The single car we'd put the user's money on, plus how sure we are and what's still unknown. */
data class BestMatch(
    val top: ScoredCar,
    val runnerUp: ScoredCar? = null,
    val confidence: Int,
    val ask: Ask? = null,
    val suggestions: List<String> = emptyList(),
)

object MatchEngine {

    const val HARD_BUDGET_FACTOR = 1.3f
    const val MAX_TASTE_WEIGHT = 3f
    const val LEARNING_RATE = 0.15f
    const val TIEBREAK_WINDOW_POINTS = 3
    private val typicalConsumption = mapOf(
        FuelType.PETROL to 7.0,
        FuelType.DIESEL to 5.8,
        FuelType.HYBRID to 5.0,
        FuelType.EV to 17.0,
    )

    fun annualCostEur(car: Car): Int? {
        val consumption = typicalConsumption[car.fuelType] ?: return null
        val energyPerUnit = when (car.fuelType) {
            FuelType.EV -> 0.25f
            FuelType.DIESEL -> 1.55f
            else -> 1.65f
        }
        return ((consumption * energyPerUnit).toFloat() * 150f).toInt() + 350
    }

    fun priceBucket(priceEur: Int): String = when {
        priceEur < 15000 -> "under 15k"
        priceEur < 30000 -> "15-30k"
        priceEur < 50000 -> "30-50k"
        else -> "50k+"
    }

    fun excluded(car: Car, prefs: UserPrefs): Boolean {
        val price = car.priceEur
        if (price != null && price > prefs.budgetEur * HARD_BUDGET_FACTOR) return true
        if (prefs.gearbox != null && car.gearbox != null && car.gearbox != prefs.gearbox) return true
        if (prefs.minYear != null && car.year != null && car.year < prefs.minYear) return true
        if (prefs.maxMileageKm != null && car.mileageKm != null && car.mileageKm > prefs.maxMileageKm) return true
        if (prefs.minPowerHp != null && car.powerHp != null && car.powerHp < prefs.minPowerHp) return true
        return false
    }

    fun tasteWeight(swipes: Int): Float = MAX_TASTE_WEIGHT * min(1f, swipes / 10f)

    fun score(car: Car, prefs: UserPrefs, affinity: Map<String, Float>, tasteW: Float): ScoredCar {
        val comps = buildList {
            budget(car, prefs)?.let { add(it) }
            add(usage(car, prefs))
            cost(car, prefs)?.let { add(it) }
            mileage(car)?.let { add(it) }
            space(car, prefs)?.let { add(it) }
            performance(car, prefs)?.let { add(it) }
            eco(car, prefs)?.let { add(it) }
            funFactor(car, prefs)?.let { add(it) }
            fuel(car, prefs)?.let { add(it) }
            gearbox(car, prefs)?.let { add(it) }
            freshness(car)?.let { add(it) }
            brand(car, prefs)?.let { add(it) }
            bodyStyle(car, prefs)?.let { add(it) }
            taste(car, affinity, tasteW)?.let { add(it) }
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

    /**
     * The user pressed "get my match car": pick the single best car, say how sure we are,
     * and — while we're below the confidence bar — ask the one question whose answer
     * would most likely change the pick, with chips built from what's actually in the pool.
     */
    fun bestMatch(
        cars: List<Car>,
        prefs: UserPrefs,
        affinity: Map<String, Float>,
        tasteW: Float,
        swipes: Int,
        answered: Set<String>,
    ): BestMatch? {
        val ranked = rank(cars, prefs, affinity, tasteW)
        if (ranked.isEmpty()) return null
        val top = ranked.first()
        // A sparse listing is a weak "match car" answer: when scores are close, prefer the
        // car that tells the buyer more. Price trumps everything (a match with no price is
        // no answer at all); after that, more documented fields win.
        var pickedIdx = 0
        var bestKey = tiebreakKey(top.car)
        for (i in 1 until ranked.size) {
            val cand = ranked[i]
            if (top.score - cand.score > TIEBREAK_WINDOW_POINTS) break
            val key = tiebreakKey(cand.car)
            if (key > bestKey) {
                bestKey = key
                pickedIdx = i
            }
        }
        val picked = ranked[pickedIdx]
        val runnerUp = ranked.firstOrNull { it !== picked }
        // Confidence = 50% answered questions + 25% swipe learning + 25% gap to the runner-up.
        val answersPart = Refine.sharpness(answered) / 100f * 0.5f
        val swipesPart = tasteW / MAX_TASTE_WEIGHT * 0.25f
        val separationPart = if (runnerUp == null) 0.25f
        else ((picked.score - runnerUp.score).toFloat() / 15f).coerceIn(0f, 1f) * 0.25f
        val confidence = ((answersPart + swipesPart + separationPart) * 100).toInt().coerceIn(0, 100)
        val suggestions = Refine.EXTRA.filter { it !in answered }.map { suggestLabel(it) }
        return BestMatch(
            top = picked,
            runnerUp = runnerUp,
            confidence = confidence,
            ask = if (confidence < 85) nextAsk(ranked, answered) else null,
            suggestions = suggestions,
        )
    }

    private fun tiebreakKey(c: Car): Int = (if (c.priceEur != null) 8 else 0) + documentedFields(c)

    private fun documentedFields(c: Car): Int =
        listOf(c.mileageKm, c.year, c.bodyType, c.fuelType, c.powerHp, c.engine, c.gearbox)
            .count { it != null }

    private fun suggestLabel(id: String): String = when (id) {
        "year" -> "Set a minimum year"
        "bodies" -> "Pick body styles"
        "brands" -> "Pick trusted brands"
        "power" -> "Set minimum power"
        "mileage" -> "Set a mileage limit"
        else -> id
    }

    /** Ask about the dimension with the most spread in the current candidate pool. */
    private fun nextAsk(ranked: List<ScoredCar>, answered: Set<String>): Ask? {
        val pool = ranked.take(15)
        if (pool.size < 3) return null
        val bodies = pool.mapNotNull { it.car.bodyType }
        if ("bodies" !in answered && bodies.distinct().size >= 2) {
            val options = bodies.groupingBy { it }.eachCount().entries
                .sortedByDescending { it.value }.take(4).map { it.key }
                .map { b -> AskOption(b.label()) { p -> p.copy(likedBodies = setOf(b)) } }
            return Ask("bodies", "Which body style speaks to you most?", options)
        }
        val hps = pool.mapNotNull { it.car.powerHp }
        if ("power" !in answered && hps.size >= 3 && (hps.max() - hps.min()) >= 80) {
            val options = listOf(100, 150, 200)
                .filter { band -> hps.count { it >= band } >= 2 }
                .map { band -> AskOption("$band+ hp") { p -> p.copy(minPowerHp = band) } }
            if (options.isNotEmpty()) return Ask("power", "How much punch do you want?", options)
        }
        val kms = pool.mapNotNull { it.car.mileageKm }
        if ("mileage" !in answered && (kms.maxOrNull() ?: 0) - (kms.minOrNull() ?: 0) >= 60_000) {
            val options = buildList {
                if (kms.count { it <= 80_000 } >= 2) add(AskOption("Under 80 000 km") { p -> p.copy(maxMileageKm = 80_000) })
                if (kms.count { it <= 150_000 } >= 2) add(AskOption("Under 150 000 km") { p -> p.copy(maxMileageKm = 150_000) })
            }
            if (options.isNotEmpty()) return Ask("mileage", "How many km is too many for you?", options)
        }
        val years = pool.mapNotNull { it.car.year }
        if ("year" !in answered && years.size >= 3 && (years.max() - years.min()) >= 4) {
            val options = listOf(2015, 2018, 2020)
                .filter { y -> years.count { it >= y } >= 2 }
                .map { y -> AskOption("$y or newer") { p -> p.copy(minYear = y) } }
            if (options.isNotEmpty()) return Ask("year", "How new should it be?", options)
        }
        val brands = pool.mapNotNull { it.car.brand }
        if ("brands" !in answered && brands.distinct().size >= 3) {
            val options = brands.groupingBy { it }.eachCount().entries
                .sortedByDescending { it.value }.take(3).map { it.key }
                .map { b -> AskOption(b) { p -> p.copy(likedBrands = setOf(b)) } }
            return Ask("brands", "Which brand do you trust most?", options)
        }
        return null
    }

    fun deck(
        cars: List<Car>,
        prefs: UserPrefs,
        affinity: Map<String, Float>,
        tasteW: Float,
        seen: Set<String>,
        random: Random = Random(42),
        count: Int = 20,
    ): List<Car> {
        var pool = cars.filter { it.id !in seen && !excluded(it, prefs) && it.photos.isNotEmpty() }
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
        val keys = buildList {
            car.bodyType?.let { add("body:$it") }
            car.fuelType?.let { add("fuel:$it") }
            car.priceEur?.let { add("price:${priceBucket(it)}") }
        }
        if (keys.isEmpty()) return affinity
        val out = affinity.toMutableMap()
        for (k in keys) out[k] = ((out[k] ?: 0f) + if (liked) lr else -lr).coerceIn(-1f, 1f)
        return out
    }

    private fun budget(car: Car, prefs: UserPrefs): Component {
        val price = car.priceEur
        if (price == null) return Component("budget", "Budget", 0.5f, 2f)
        val b = prefs.budgetEur.toFloat()
        val v = if (price <= b) 1f else (1f - (price - b) / (0.3f * b)).coerceIn(0f, 1f)
        val reason = if (price <= b) "${(b - price).toInt().groupThousands()} € under budget" else null
        return Component("budget", "Budget", v, 2f, reason)
    }

    private fun usage(car: Car, prefs: UserPrefs): Component {
        val body = car.bodyType
        if (body == null) return Component("usage", "Lifestyle fit", 0.5f, 2.5f)
        var v = when (prefs.usage) {
            Usage.COMMUTE -> when (body) {
                BodyType.HATCHBACK, BodyType.SEDAN -> 1f
                BodyType.WAGON -> 0.8f
                BodyType.SUV -> 0.6f
                BodyType.COUPE, BodyType.CONVERTIBLE -> 0.4f
                BodyType.VAN -> 0.3f
                BodyType.PICKUP -> 0.2f
            }
            Usage.FAMILY -> when (body) {
                BodyType.WAGON, BodyType.SUV -> 1f
                BodyType.VAN -> 0.9f
                BodyType.SEDAN -> 0.7f
                BodyType.HATCHBACK -> 0.6f
                BodyType.PICKUP -> 0.4f
                BodyType.COUPE -> 0.1f
                BodyType.CONVERTIBLE -> 0.05f
            }
            Usage.SPORT -> when (body) {
                BodyType.COUPE -> 1f
                BodyType.CONVERTIBLE -> 0.95f
                BodyType.SEDAN -> 0.4f
                BodyType.HATCHBACK -> 0.35f
                BodyType.WAGON -> 0.3f
                BodyType.SUV -> 0.2f
                BodyType.PICKUP -> 0.1f
                BodyType.VAN -> 0.05f
            }
            Usage.ADVENTURE -> when (body) {
                BodyType.SUV -> 1f
                BodyType.PICKUP -> 0.95f
                BodyType.WAGON -> 0.6f
                BodyType.VAN -> 0.35f
                BodyType.HATCHBACK -> 0.3f
                BodyType.SEDAN -> 0.25f
                BodyType.COUPE, BodyType.CONVERTIBLE -> 0.1f
            }
            Usage.CITY -> when (body) {
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
        if (prefs.usage == Usage.FAMILY && car.sevenSeatHint()) v = (v + 0.1f).coerceAtMost(1f)
        val reason = if (v >= 0.75f) "Great fit for ${prefs.usage.usageLabel()}" else null
        return Component("usage", "Lifestyle fit", v, 2.5f, reason)
    }

    private fun cost(car: Car, prefs: UserPrefs): Component? {
        val annual = annualCostEur(car) ?: return null
        val v = (1f - (annual - 600f) / 2200f).coerceIn(0f, 1f)
        val reason = if (annual <= 1100 && prefs.weights.runningCost >= 0.3f) "Low running costs (~${annual.groupThousands()} €/yr)" else null
        return Component("cost", "Running costs", v, 1f + 2f * prefs.weights.runningCost, reason)
    }

    private fun mileage(car: Car): Component? {
        val km = car.mileageKm ?: return null
        val v = (1f - km / 250000f).coerceIn(0f, 1f)
        val reason = if (km < 80_000) "Low mileage: ${km.groupThousands()} km" else null
        return Component("mileage", "Mileage", v, 1.2f, reason)
    }

    private fun space(car: Car, prefs: UserPrefs): Component? {
        val body = car.bodyType ?: return null
        val bootProxy = when (body) {
            BodyType.WAGON, BodyType.SUV, BodyType.VAN, BodyType.PICKUP -> 1f
            BodyType.SEDAN, BodyType.HATCHBACK -> 0.6f
            BodyType.COUPE, BodyType.CONVERTIBLE -> 0.25f
        }
        val seatProxy = if (car.sevenSeatHint()) 1f else 0.65f
        val v = bootProxy * 0.7f + seatProxy * 0.3f
        val reason = if (v >= 0.85f) "Roomy: ${body.name.lowercase().replaceFirstChar { it.uppercase() }}" else null
        return Component("space", "Space", v, 1f + 2f * prefs.weights.space, reason)
    }

    private fun performance(car: Car, prefs: UserPrefs): Component? {
        val hp = car.powerHp ?: return null
        val v = ((hp - 60f) / 290f).coerceIn(0f, 1f)
        val reason = if (v >= 0.6f) "Quick: $hp hp" else null
        return Component("performance", "Performance", v, 1f + 2f * prefs.weights.performance, reason)
    }

    private fun eco(car: Car, prefs: UserPrefs): Component? {
        val fuel = car.fuelType ?: return null
        val v = when (fuel) {
            FuelType.EV -> 1f
            FuelType.HYBRID -> 0.75f
            FuelType.PETROL -> 0.45f
            FuelType.DIESEL -> 0.35f
        }
        val reason = if (fuel == FuelType.EV && prefs.weights.eco >= 0.3f) "Zero tailpipe emissions" else null
        return Component("eco", "Eco", v, 1f + 2f * prefs.weights.eco, reason)
    }

    private fun funFactor(car: Car, prefs: UserPrefs): Component? {
        val hpNorm = car.powerHp?.let { ((it - 60f) / 290f).coerceIn(0f, 1f) }
        val body = car.bodyType
        if (hpNorm == null && body == null) return null
        var v = 0.4f + 0.25f * (hpNorm ?: 0.3f)
        if (body == BodyType.COUPE || body == BodyType.CONVERTIBLE) v += 0.2f
        v = v.coerceIn(0f, 1f)
        val reason = if (v >= 0.75f) "Proper driver's car" else null
        return Component("fun", "Driving fun", v, 1f + 2f * prefs.weights.drivingFun, reason)
    }

    private fun fuel(car: Car, prefs: UserPrefs): Component? {
        val fuel = car.fuelType ?: return null
        if (prefs.fuelPrefs.isEmpty()) return null
        val v = if (fuel in prefs.fuelPrefs) 1f else 0.2f
        val reason = if (v == 1f) "Matches your fuel preference" else null
        return Component("fuel", "Fuel choice", v, 1.5f, reason)
    }

    private fun gearbox(car: Car, prefs: UserPrefs): Component? {
        val pref = prefs.gearbox ?: return null
        val gb = car.gearbox ?: return null
        val v = if (gb == pref) 1f else 0f
        val reason = if (v == 1f && pref == Gearbox.AUTOMATIC) "Automatic gearbox" else null
        return Component("gearbox", "Gearbox", v, 1f, reason)
    }

    private fun freshness(car: Car): Component? {
        val year = car.year ?: return null
        val v = ((year - 2003f) / 21f).coerceIn(0f, 1f)
        val reason = if (year >= 2020) "Nearly new: $year" else null
        return Component("year", "Freshness", v, 0.8f, reason)
    }

    private fun brand(car: Car, prefs: UserPrefs): Component? {
        if (prefs.likedBrands.isEmpty()) return null
        val b = car.brand ?: return null
        val v = if (b in prefs.likedBrands) 1f else 0.3f
        val reason = if (v == 1f) "You like $b" else null
        return Component("brand", "Brand", v, 1.5f, reason)
    }

    private fun bodyStyle(car: Car, prefs: UserPrefs): Component? {
        if (prefs.likedBodies.isEmpty()) return null
        val body = car.bodyType ?: return null
        val v = if (body in prefs.likedBodies) 1f else 0.3f
        val reason = if (v == 1f) "${body.name.lowercase().replaceFirstChar { it.uppercase() }} is on your list" else null
        return Component("bodies", "Body style", v, 1.5f, reason)
    }

    private fun taste(car: Car, affinity: Map<String, Float>, weight: Float): Component? {
        if (weight <= 0f) return null
        val keys = buildList {
            car.bodyType?.let { add("body:$it") }
            car.fuelType?.let { add("fuel:$it") }
            car.priceEur?.let { add("price:${priceBucket(it)}") }
        }
        if (keys.isEmpty()) return null
        val v = keys.map { ((affinity[it] ?: 0f) + 1f) / 2f }.average().toFloat()
        val reason = if (v >= 0.65f) "Similar to cars you liked" else null
        return Component("taste", "Your taste", v, weight, reason)
    }
}

private fun Car.sevenSeatHint(): Boolean =
    Regex("7\\s*viet|7-seat|7 seat", RegexOption.IGNORE_CASE).containsMatchIn(title)
