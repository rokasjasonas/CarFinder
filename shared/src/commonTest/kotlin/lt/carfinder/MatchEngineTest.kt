package lt.carfinder

import lt.carfinder.data.Catalog
import lt.carfinder.engine.MatchEngine
import lt.carfinder.model.BodyType
import lt.carfinder.model.Drive
import lt.carfinder.model.FuelType
import lt.carfinder.model.Gearbox
import lt.carfinder.model.Usage
import lt.carfinder.model.UserPrefs
import lt.carfinder.model.Weights
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchEngineTest {

    private val familyPrefs = UserPrefs(
        budgetEur = 35000,
        usage = Usage.FAMILY,
        minSeats = 5,
        gearbox = Gearbox.AUTOMATIC,
        fuelPrefs = emptySet(),
        weights = Weights(space = 1f, runningCost = 0.5f),
    )

    private fun car(
        id: String = "test-car",
        price: Int = 30000,
        seats: Int = 5,
        body: BodyType = BodyType.WAGON,
        fuel: FuelType = FuelType.PETROL,
        gearbox: Gearbox = Gearbox.AUTOMATIC,
    ) = lt.carfinder.model.Car(
        id = id, brand = "Test", model = id, year = 2022, priceEur = price,
        bodyType = body, fuelType = fuel, gearbox = gearbox, seats = seats,
        powerHp = 150, trunkL = 600, consumption = 6.0, drive = Drive.FWD,
        emoji = "🚗", accent = 0xFF3B5BDB,
    )

    @Test
    fun overBudgetCarsAreExcluded() {
        val porsche = Catalog.car("porsche-911")!!
        assertTrue(MatchEngine.excluded(porsche, familyPrefs))
        assertTrue(MatchEngine.rank(Catalog.cars, familyPrefs, emptyMap(), 0f).none { it.car.id == "porsche-911" })
    }

    @Test
    fun seatRequirementExcludesTwoSeatCars() {
        val mx5 = Catalog.car("mazda-mx5")!!
        assertTrue(MatchEngine.excluded(mx5, familyPrefs))
    }

    @Test
    fun gearboxPreferenceIsHardFilter() {
        val manual = car(gearbox = Gearbox.MANUAL)
        assertTrue(MatchEngine.excluded(manual, familyPrefs))
    }

    @Test
    fun budgetTaperPenalisesOverBudgetCars() {
        val within = MatchEngine.score(car(price = 30000), familyPrefs, emptyMap(), 0f)
        val stretch = MatchEngine.score(car(price = 40000), familyPrefs, emptyMap(), 0f)
        assertTrue(within.score > stretch.score)
        val budgetComponent = stretch.components.first { it.key == "budget" }
        assertTrue(budgetComponent.value in 0.01f..0.99f)
    }

    @Test
    fun likingAnSuvRaisesAffinityForOtherSuvs() {
        val prefs = familyPrefs
        val suv = car(id = "suv-a", body = BodyType.SUV, price = 20000)
        val otherSuv = car(id = "suv-b", body = BodyType.SUV, price = 25000)
        val wagon = car(id = "wagon", body = BodyType.WAGON, price = 25000)

        val before = MatchEngine.score(otherSuv, prefs, emptyMap(), tasteW = 3f).score
        val learned = MatchEngine.learn(emptyMap(), suv, liked = true)
        val after = MatchEngine.score(otherSuv, prefs, learned, tasteW = 3f).score
        val wagonAfter = MatchEngine.score(wagon, prefs, learned, tasteW = 3f).score

        assertTrue(after > before, "liking an SUV should raise the score of another SUV")
        assertTrue(after - before > wagonAfter - before, "learned taste should favour SUVs over wagons")
    }

    @Test
    fun tasteWeightRampsUpWithSwipes() {
        assertEquals(0f, MatchEngine.tasteWeight(0))
        assertTrue(MatchEngine.tasteWeight(5) < MatchEngine.tasteWeight(10))
        assertEquals(MatchEngine.MAX_TASTE_WEIGHT, MatchEngine.tasteWeight(25))
    }

    @Test
    fun deckExcludesSeenAndExcludedCars() {
        val all = Catalog.cars
        val seen = all.map { it.id }.toSet()
        assertTrue(MatchEngine.deck(all, familyPrefs, emptyMap(), 1f, seen).isEmpty())
        val deck = MatchEngine.deck(all, familyPrefs, emptyMap(), 1f, emptySet(), count = 10)
        assertEquals(10, deck.size)
        assertTrue(deck.none { MatchEngine.excluded(it, familyPrefs) })
    }

    @Test
    fun rankIsSortedDescending() {
        val ranked = MatchEngine.rank(Catalog.cars, familyPrefs, emptyMap(), 0f)
        assertTrue(ranked.size > 5)
        assertEquals(ranked.map { it.score }.sortedDescending(), ranked.map { it.score })
    }

    @Test
    fun evScoresBetterOnEcoThanDiesel() {
        val ev = MatchEngine.score(car(id = "ev", fuel = FuelType.EV), familyPrefs, emptyMap(), 0f)
        val diesel = MatchEngine.score(car(id = "d", fuel = FuelType.DIESEL), familyPrefs, emptyMap(), 0f)
        val evEco = ev.components.first { it.key == "eco" }.value
        val dEco = diesel.components.first { it.key == "eco" }.value
        assertTrue(evEco > dEco)
    }
}
