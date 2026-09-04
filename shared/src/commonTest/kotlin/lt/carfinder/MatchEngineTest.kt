package lt.carfinder

import lt.carfinder.engine.MatchEngine
import lt.carfinder.model.BodyType
import lt.carfinder.model.Car
import lt.carfinder.model.FuelType
import lt.carfinder.model.Gearbox
import lt.carfinder.model.Source
import lt.carfinder.model.Usage
import lt.carfinder.model.UserPrefs
import lt.carfinder.model.Weights
import lt.carfinder.sites.Sites
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchEngineTest {

    private val familyPrefs = UserPrefs(
        budgetEur = 35000,
        usage = Usage.FAMILY,
        gearbox = Gearbox.AUTOMATIC,
        fuelPrefs = emptySet(),
        weights = Weights(space = 1f, runningCost = 0.5f),
    )

    private fun car(
        id: String = "ap-1",
        price: Int? = 30000,
        mileage: Int? = 100000,
        body: BodyType? = BodyType.WAGON,
        fuel: FuelType? = FuelType.PETROL,
        gearbox: Gearbox? = Gearbox.AUTOMATIC,
        hp: Int? = 150,
    ) = Car(
        id = id, source = Source.AUTOPLIUS, url = "https://autoplius.lt/skelbimai/test-$id.html",
        title = "Test $id", priceEur = price, year = 2021, mileageKm = mileage,
        fuelType = fuel, gearbox = gearbox, bodyType = body, powerHp = hp,
        photos = listOf("https://img.example/$id.jpg"),
    )

    @Test
    fun overBudgetCarsAreExcluded() {
        assertTrue(MatchEngine.excluded(car(price = 50000), familyPrefs))
        assertTrue(MatchEngine.excluded(car(price = 128000), familyPrefs))
    }

    @Test
    fun unknownPriceIsNeverExcluded() {
        assertTrue(!MatchEngine.excluded(car(price = null), familyPrefs))
    }

    @Test
    fun knownGearboxMismatchIsHardFilterButUnknownPasses() {
        assertTrue(MatchEngine.excluded(car(gearbox = Gearbox.MANUAL), familyPrefs))
        assertTrue(!MatchEngine.excluded(car(gearbox = null), familyPrefs))
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
        val suv = car(id = "ap-2", body = BodyType.SUV, price = 20000)
        val otherSuv = car(id = "ap-3", body = BodyType.SUV, price = 25000)
        val wagon = car(id = "ap-4", body = BodyType.WAGON, price = 25000)

        val before = MatchEngine.score(otherSuv, familyPrefs, emptyMap(), tasteW = 3f).score
        val learned = MatchEngine.learn(emptyMap(), suv, liked = true)
        val after = MatchEngine.score(otherSuv, familyPrefs, learned, tasteW = 3f).score
        val wagonAfter = MatchEngine.score(wagon, familyPrefs, learned, tasteW = 3f).score

        assertTrue(after > before, "liking an SUV should raise the score of another SUV")
        assertTrue(after - before > wagonAfter - before, "learned taste should favour SUVs over wagons")
    }

    @Test
    fun sparseCarStillGetsScored() {
        val sparse = Car(
            id = "ag-9", source = Source.AUTOGIDAS, url = "https://autogidas.lt/skelbimai/x-12345.html",
            title = "Auto", photos = listOf("p.jpg"),
        )
        val sc = MatchEngine.score(sparse, familyPrefs, emptyMap(), 0f)
        assertTrue(sc.score > 0)
        assertTrue(sc.components.any { it.key == "usage" })
        assertTrue(sc.components.none { it.key == "performance" })
    }

    @Test
    fun tasteWeightRampsUpWithSwipes() {
        assertEquals(0f, MatchEngine.tasteWeight(0))
        assertTrue(MatchEngine.tasteWeight(5) < MatchEngine.tasteWeight(10))
        assertEquals(MatchEngine.MAX_TASTE_WEIGHT, MatchEngine.tasteWeight(25))
    }

    @Test
    fun deckExcludesSeenAndCarsWithoutPhotos() {
        val cars = listOf(car("ap-1"), car("ap-2"), Car(id = "ag-3", source = Source.AUTOGIDAS, url = "u", title = "no photo", photos = emptyList()))
        assertEquals(0, MatchEngine.deck(cars, familyPrefs, emptyMap(), 1f, seen = setOf("ap-1", "ap-2", "ag-3")).size)
        val deck = MatchEngine.deck(cars, familyPrefs, emptyMap(), 1f, emptySet())
        assertEquals(2, deck.size)
        assertTrue(deck.none { it.id == "ag-3" })
    }

    @Test
    fun rankIsSortedDescending() {
        val cars = (1..8).map { car("ap-$it", price = 10000 + it * 1000) }
        val ranked = MatchEngine.rank(cars, familyPrefs, emptyMap(), 0f)
        assertEquals(8, ranked.size)
        assertEquals(ranked.map { it.score }.sortedDescending(), ranked.map { it.score })
    }

    @Test
    fun evScoresBetterOnEcoThanDiesel() {
        val ev = MatchEngine.score(car(id = "ap-10", fuel = FuelType.EV), familyPrefs, emptyMap(), 0f)
        val diesel = MatchEngine.score(car(id = "ap-11", fuel = FuelType.DIESEL), familyPrefs, emptyMap(), 0f)
        val evEco = ev.components.first { it.key == "eco" }.value
        val dEco = diesel.components.first { it.key == "eco" }.value
        assertTrue(evEco > dEco)
    }
}

class SitesTest {

    @Test
    fun listingIdMatchesBothSites() {
        assertEquals("12345678", Sites.listingId("https://autoplius.lt/skelbimai/bmw-530d-12345678.html"))
        assertEquals("7654321", Sites.listingId("https://autogidas.lt/skelbimai/vw-golf-7654321.html"))
        assertNull(Sites.listingId("https://autoplius.lt/skelbimai/naudoti-automobiliai"))
    }

    @Test
    fun detectsSiteFromUrl() {
        assertEquals(Source.AUTOGIDAS, Sites.detect("https://autogidas.lt/automobiliai/").source)
        assertEquals(Source.AUTOPLIUS, Sites.detect("https://autoplius.lt/").source)
    }

    @Test
    fun normalizesLithuanianFuelTerms() {
        assertEquals(FuelType.PETROL, Sites.normalizeFuel("Benzinas"))
        assertEquals(FuelType.PETROL, Sites.normalizeFuel("Benzinas / Dujos"))
        assertEquals(FuelType.DIESEL, Sites.normalizeFuel("Dyzelinas"))
        assertEquals(FuelType.EV, Sites.normalizeFuel("Elektra"))
        assertEquals(FuelType.HYBRID, Sites.normalizeFuel("Hibridas (Plug-in)"))
        assertNull(Sites.normalizeFuel("???"))
    }

    @Test
    fun normalizesGearboxTerms() {
        assertEquals(Gearbox.AUTOMATIC, Sites.normalizeGearbox("Automatinė"))
        assertEquals(Gearbox.MANUAL, Sites.normalizeGearbox("Mechaninė"))
        assertNull(Sites.normalizeGearbox("n/a"))
    }

    @Test
    fun infersBodyTypeFromTitle() {
        assertEquals(BodyType.WAGON, Sites.inferBodyType("BMW 530d xDrive Touring"))
        assertEquals(BodyType.WAGON, Sites.inferBodyType("Škoda Octavia Universalas"))
        assertEquals(BodyType.COUPE, Sites.inferBodyType("BMW M4 Coupé"))
        assertEquals(BodyType.CONVERTIBLE, Sites.inferBodyType("Mazda MX-5 Kabrioletas"))
        assertEquals(BodyType.SUV, Sites.inferBodyType("Toyota RAV4 SUV 4x4"))
        assertEquals(BodyType.PICKUP, Sites.inferBodyType("Ford Ranger Pick-up"))
        assertNull(Sites.inferBodyType("VW Golf 1.4 TSI"))
    }

    @Test
    fun parsesPowerFromKwAndAg() {
        assertEquals(204, Sites.parsePowerHp("150 kW"))
        assertEquals(135, Sites.parsePowerHp("135 AG"))
        assertNull(Sites.parsePowerHp("2.0 TDI"))
    }

    @Test
    fun parsesSearchHarvestPayload() {
        val raw = """
            {"items":[{"id":"7654321","url":"https://autogidas.lt/skelbimai/bmw-320-7654321.html","title":"BMW 320 Universalas",
            "price":15900,"year":2018,"mileageKm":178000,"fuel":"Dyzelinas","gearbox":"Automatinė","engine":"140 kW",
            "photos":["https://img.autogidas.lt/x.jpg"]}]}
        """.trimIndent()
        val captured = Sites.parse(raw)
        val cars = (captured as Sites.Captured.Many).cars
        assertEquals(1, cars.size)
        val c = cars[0]
        assertEquals("7654321", c.id)
        assertEquals(Source.AUTOGIDAS, c.source)
        assertEquals(15900, c.priceEur)
        assertEquals(2018, c.year)
        assertEquals(178000, c.mileageKm)
        assertEquals(FuelType.DIESEL, c.fuelType)
        assertEquals(Gearbox.AUTOMATIC, c.gearbox)
        assertEquals(BodyType.WAGON, c.bodyType)
        assertEquals(190, c.powerHp)
    }
}
