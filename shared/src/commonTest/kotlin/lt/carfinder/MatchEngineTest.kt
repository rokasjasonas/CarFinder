package lt.carfinder

import lt.carfinder.engine.MatchEngine
import lt.carfinder.engine.Refine
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
import kotlin.test.assertFalse
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
        year: Int = 2021,
    ) = Car(
        id = id, source = Source.AUTOPLIUS, url = "https://autoplius.lt/skelbimai/test-$id.html",
        title = "Test $id", priceEur = price, year = year, mileageKm = mileage,
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

        val before = MatchEngine.score(otherSuv, familyPrefs, emptyMap(), tasteW = 3f).components.first { it.key == "taste" }.value
        val learned = MatchEngine.learn(emptyMap(), suv, liked = true)
        val after = MatchEngine.score(otherSuv, familyPrefs, learned, tasteW = 3f).components.first { it.key == "taste" }.value
        val wagonAfter = MatchEngine.score(wagon, familyPrefs, learned, tasteW = 3f).components.first { it.key == "taste" }.value

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

    @Test
    fun refinedHardFiltersExcludeKnownValuesButPassUnknowns() {
        val strict = familyPrefs.copy(minYear = 2015, maxMileageKm = 150_000, minPowerHp = 120)
        assertTrue(MatchEngine.excluded(car(year = 2010), strict))
        assertTrue(MatchEngine.excluded(car(mileage = 200_000), strict))
        assertTrue(MatchEngine.excluded(car(hp = 90), strict))
        assertTrue(!MatchEngine.excluded(car(), strict))
        val unknowns = Car(id = "ap-20", source = Source.AUTOPLIUS, url = "u", title = "Mystery", photos = listOf("p.jpg"))
        assertTrue(!MatchEngine.excluded(unknowns, strict), "cars with unknown fields must never be excluded")
    }

    @Test
    fun pricelessCarsStillExcludedByKnownFields() {
        val strict = familyPrefs.copy(minYear = 2015, maxMileageKm = 150_000, minPowerHp = 120)
        assertTrue(MatchEngine.excluded(car("ap-30", price = null, year = 2010), strict))
        assertTrue(MatchEngine.excluded(car("ap-31", price = null, mileage = 200_000), strict))
        assertTrue(MatchEngine.excluded(car("ap-32", price = null, hp = 90), strict))
        assertTrue(!MatchEngine.excluded(car("ap-33", price = null), strict))
    }

    @Test
    fun likedBrandAndBodyBoostScores() {
        val bmw = car(id = "ap-21").copy(title = "BMW 320d Touring")
        val opel = car(id = "ap-22").copy(title = "Opel Astra Sports Tourer")
        val tuned = familyPrefs.copy(likedBrands = setOf("BMW"), likedBodies = setOf(BodyType.WAGON))
        val bmwScore = MatchEngine.score(bmw, tuned, emptyMap(), 0f)
        val opelScore = MatchEngine.score(opel, tuned, emptyMap(), 0f)
        assertTrue(bmwScore.score > opelScore.score, "liked brand should outrank an unlisted brand")
        assertTrue(bmwScore.components.any { it.key == "brand" && it.reason != null })
        val neutral = MatchEngine.score(bmw, familyPrefs, emptyMap(), 0f)
        assertTrue(!neutral.components.any { it.key == "brand" }, "brand component only appears once brands are answered")
    }

    @Test
    fun brandIsExtractedFromTitle() {
        assertEquals("BMW", car().copy(title = "BMW 320d M Sport").brand)
        assertEquals("Škoda", car().copy(title = "Škoda  Octavia").brand)
        assertNull(car().copy(title = "9").brand)
    }

    @Test
    fun sharpnessCountsAnsweredQuestions() {
        assertEquals(0, lt.carfinder.engine.Refine.sharpness(emptySet()))
        assertEquals(50, lt.carfinder.engine.Refine.sharpness(lt.carfinder.engine.Refine.INITIAL.toSet()))
        assertEquals(100, lt.carfinder.engine.Refine.sharpness((lt.carfinder.engine.Refine.INITIAL + lt.carfinder.engine.Refine.EXTRA).toSet()))
        assertEquals(70, lt.carfinder.engine.Refine.sharpness((lt.carfinder.engine.Refine.INITIAL + listOf("year", "brands")).toSet()))
    }

    @Test
    fun searchPageBuildsPaginatedUrls() {
        assertEquals(Sites.AUTOPLIUS.defaultSearch, Sites.AUTOPLIUS.searchPage(1))
        assertEquals("https://autoplius.lt/skelbimai/naudoti-automobiliai?page=3", Sites.AUTOPLIUS.searchPage(3))
        assertEquals("https://autogidas.lt/skelbimai/automobiliai/?page=2", Sites.AUTOGIDAS.searchPage(2))
    }

    @Test
    fun bestMatchPicksTopRankedCarWithRunnerUp() {
        val cars = listOf(
            car(id = "ap-1", body = BodyType.WAGON, price = 25000),
            car(id = "ap-2", body = BodyType.SUV, price = 30000),
            car(id = "ap-3", body = BodyType.HATCHBACK, price = 35000),
        )
        val best = MatchEngine.bestMatch(cars, familyPrefs, emptyMap(), 0f, 0, emptySet())!!
        val ranked = MatchEngine.rank(cars, familyPrefs, emptyMap(), 0f)
        assertEquals(ranked.first().car.id, best.top.car.id)
        assertEquals(ranked.first().score, best.top.score)
        assertEquals(ranked.getOrNull(1)?.car?.id, best.runnerUp?.car?.id)
    }

    @Test
    fun confidenceGrowsWithAnswersAndSwipes() {
        val cars = List(4) { i ->
            car(id = "ap-$i", body = if (i % 2 == 0) BodyType.WAGON else BodyType.SUV,
                price = 20000 + i * 1000, hp = 100 + i * 30, year = 2015 + i, mileage = 60_000 + i * 20_000)
        }
        val all = (Refine.INITIAL + Refine.EXTRA).toSet()
        val bare = MatchEngine.bestMatch(cars, familyPrefs, emptyMap(), 0f, 0, emptySet())!!
        val tuned = MatchEngine.bestMatch(cars, familyPrefs, emptyMap(), MatchEngine.MAX_TASTE_WEIGHT, 10, all)!!
        assertTrue(bare.confidence < tuned.confidence)
        assertTrue(bare.confidence <= 25, "no answers, no swipes = low confidence")
        assertTrue(tuned.confidence >= 75)
        assertEquals(5, bare.suggestions.size)
        assertTrue(tuned.suggestions.isEmpty())
        assertTrue(tuned.ask == null)
    }

    @Test
    fun matchCarPrefersBetterDocumentedCarWithinWindow() {
        val sparse = car(id = "ag-1", price = 20000, body = BodyType.WAGON, year = 2022, mileage = null, hp = null)
        val rich = car(id = "ap-2", price = 30800, body = BodyType.SUV, year = 2022, mileage = 60_000, hp = 286)
        val close = MatchEngine.bestMatch(listOf(sparse, rich), familyPrefs, emptyMap(), 0f, 0, emptySet())!!
        assertEquals("ap-2", close.top.car.id)
        assertEquals("ag-1", close.runnerUp?.car?.id)
        val far = car(id = "ap-3", price = 45000, body = BodyType.SUV, year = 2013, mileage = 240_000, hp = 70)
        val kept = MatchEngine.bestMatch(listOf(rich, far), familyPrefs, emptyMap(), 0f, 0, emptySet())!!
        assertEquals("ap-2", kept.top.car.id)
        val richPriceless = car(id = "ap-4", price = null, body = BodyType.SUV, year = 2023, mileage = 40_000, hp = 250)
        val priceWins = MatchEngine.bestMatch(listOf(richPriceless, sparse), familyPrefs, emptyMap(), 0f, 0, emptySet())!!
        assertEquals("ag-1", priceWins.top.car.id)
        assertEquals("ap-4", priceWins.runnerUp?.car?.id)
    }

    @Test
    fun asksAboutBodyStyleWhenUnansweredAndDiverse() {
        val cars = listOf(
            car(id = "ap-1", body = BodyType.WAGON, price = 20000),
            car(id = "ap-2", body = BodyType.SUV, price = 22000),
            car(id = "ap-3", body = BodyType.SUV, price = 24000),
            car(id = "ap-4", body = BodyType.HATCHBACK, price = 26000),
        )
        val ask = MatchEngine.bestMatch(cars, familyPrefs, emptyMap(), 0f, 0, emptySet())!!.ask!!
        assertEquals("bodies", ask.id)
        assertTrue(ask.options.map { it.label }.contains("SUV"))
        val withSuv = ask.options.first { it.label == "SUV" }.patch(familyPrefs)
        assertEquals(setOf(BodyType.SUV), withSuv.likedBodies)
        val next = MatchEngine.bestMatch(cars, withSuv, emptyMap(), 0f, 0, setOf("bodies"))
        assertTrue(next!!.ask == null)
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

    @Test
    fun dropsJunkWeBuyAdsAndFieldlessPayloads() {
        val raw = """
            {"items":[
              {"id":"111","url":"https://autoplius.lt/skelbimai/automobiliu-supirkimas-111.html","title":"Automobilių supirkimas","price":1000,"photos":["https://x/a.jpg"]},
              {"id":"112","url":"https://autoplius.lt/skelbimai/perkame-automobilius-112.html","title":"Perkame automobilius","price":1000,"photos":["https://x/b.jpg"]},
              {"id":"113","url":"https://autoplius.lt/skelbimai/bmw-530-113.html","title":"BMW 530","price":5000},
              {"id":"114","url":"https://autoplius.lt/skelbimai/bmw-530-114.html","title":"BMW 530 2015","year":2015,"photos":["https://x/c.jpg"]}
            ]}
        """.trimIndent()
        val cars = (Sites.parse(raw) as Sites.Captured.Many).cars
        assertEquals(listOf("114"), cars.map { it.id })
        assertEquals(2015, cars[0].year)
    }

    @Test
    fun plausibleRejectsJunkAdsAndFieldlessCars() {
        fun car(id: String, title: String, year: Int? = null, mileage: Int? = null) = Car(
            id = id, source = Source.AUTOPLIUS, url = "https://autoplius.lt/skelbimai/x-$id.html",
            title = title, priceEur = 1000, year = year, mileageKm = mileage, capturedAt = 0L,
        )
        assertTrue(Sites.plausible(car("114", "BMW 530 2015", year = 2015)))
        assertTrue(Sites.plausible(car("115", "BMW 530", mileage = 120_000)))
        assertTrue(Sites.junkTitle("Perkame automobilius"))
        assertTrue(Sites.junkTitle("Automobilių supirkimas"))
        assertTrue(Sites.junkTitle("Ieškau automobilio"))
        assertFalse(Sites.plausible(car("111", "Perkame automobilius", year = 2020)))
        assertFalse(Sites.plausible(car("113", "BMW 530")))
    }
}
