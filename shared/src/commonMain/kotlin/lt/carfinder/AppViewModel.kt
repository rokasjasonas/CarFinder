package lt.carfinder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import lt.carfinder.data.loadState
import lt.carfinder.data.saveState
import lt.carfinder.engine.MatchEngine
import lt.carfinder.engine.BestMatch
import lt.carfinder.engine.Refine
import lt.carfinder.engine.ScoredCar
import lt.carfinder.model.Car
import lt.carfinder.model.Swipe
import lt.carfinder.model.Source
import lt.carfinder.model.UserPrefs
import lt.carfinder.sites.Sites

sealed interface Tab {
    data object Discover : Tab
    data object Matches : Tab
    data object Refine : Tab
}

sealed interface Route {
    data class CarDetail(val carId: String) : Route
    data object MatchCar : Route
}

data class FetchJob(val url: String, val n: Int)

class AppViewModel : ViewModel() {

    var state by mutableStateOf(loadState())
        private set

    var tab by mutableStateOf<Tab>(Tab.Discover)
    val stack = mutableStateListOf<Route>()

    val hasPrefs: Boolean get() = state.prefs != null
    val swipedIds: Set<String> get() = state.swipes.map { it.carId }.toSet()

    init {
        // Purge junk cars saved by older builds (parse-time filtering only guards new payloads).
        val kept = state.listings.filter { Sites.plausible(it) }
        if (kept.size != state.listings.size) {
            state = state.copy(listings = kept)
            saveState(state)
        }
    }

    private fun tasteW(): Float = MatchEngine.tasteWeight(state.swipes.size)

    // ---- Hidden harvester: the app fetches listings itself, no browsing needed. ----

    private companion object {
        const val MAX_PAGES = 5
        const val DECK_LOW = 10
        const val FETCH_ROUND_MS = 12_000L
    }

    /** Current job for the hidden WebView; null when idle. */
    var harvester by mutableStateOf<FetchJob?>(null)
        private set

    val fetchBusy: Boolean get() = harvester != null

    private val fetchQueue = ArrayDeque<String>()
    private val pagesFetched = mutableMapOf<Source, Int>()
    private val galleryQueued = mutableSetOf<String>()
    private var jobN = 0

    private fun pump() {
        harvester = fetchQueue.removeFirstOrNull()?.let { FetchJob(it, ++jobN) }
    }

    fun harvestDone() {
        if (harvester != null) pump()
    }

    /** Keep the deck stocked: enqueue the next search page from each site when it runs low. */
    fun ensureCars() {
        if (harvester != null) return
        if (deckCount() >= DECK_LOW) return
        var enqueued = false
        for (site in Sites.ALL) {
            val page = (pagesFetched[site.source] ?: 0) + 1
            if (page > MAX_PAGES) continue
            fetchQueue.addLast(site.searchPage(page))
            pagesFetched[site.source] = page
            enqueued = true
        }
        if (enqueued) pump()
    }

    private fun deckCount(): Int {
        val prefs = state.prefs ?: return 0
        val seen = swipedIds
        return state.listings.count { it.id !in seen && !MatchEngine.excluded(it, prefs) && it.photos.isNotEmpty() }
    }

    /** Search-card harvests carry one thumbnail; queue the listing page once to gain the full gallery. */
    fun requestGallery(car: Car): Boolean =
        car.photos.size <= 1 && !galleryQueued.contains(car.id) && galleryQueued.add(car.id) && run {
            fetchQueue.addLast(car.url)
            if (harvester == null) pump()
            true
        }

    fun onHarvestPayload(raw: String) = onPayload(raw)

    // ---- Prefs & refinement ----

    fun completeQuiz(prefs: UserPrefs) {
        state = state.copy(prefs = prefs, answered = state.answered + Refine.INITIAL.toSet())
        saveState(state)
        ensureCars()
    }

    fun answerRefine(id: String, patch: (UserPrefs) -> UserPrefs) {
        val prefs = state.prefs ?: return
        state = state.copy(prefs = patch(prefs), answered = state.answered + id)
        saveState(state)
    }

    fun retakeQuiz() {
        state = state.copy(prefs = null, answered = emptySet())
        saveState(state)
    }

    fun resetTaste() {
        state = state.copy(affinity = emptyMap(), swipes = emptyList())
        saveState(state)
    }

    val sharpness: Int get() = Refine.sharpness(state.answered)

    fun swipe(carId: String, liked: Boolean) {
        val car = car(carId) ?: return
        state = state.copy(
            swipes = state.swipes + Swipe(carId, liked),
            affinity = MatchEngine.learn(state.affinity, car, liked),
        )
        saveState(state)
    }

    /** Payload from the hidden harvester WebView (search pages or prefetched galleries). */
    fun onPayload(raw: String) {
        when (val c = Sites.parse(raw)) {
            is Sites.Captured.Many -> upsertListings(c.cars)
            is Sites.Captured.One -> upsertListings(listOf(c.car))
            Sites.Captured.None -> Unit
        }
    }

    private fun upsertListings(new: List<Car>) {
        if (new.isEmpty()) return
        val byId = state.listings.associateBy { it.id }.toMutableMap()
        new.forEach { byId[it.id] = it }
        val all = byId.values.sortedByDescending { it.capturedAt }
        val trimmed = if (all.size > 400) {
            val swiped = swipedIds
            val kept = all.filter { it.id !in swiped }.ifEmpty { all }
            kept.take(400)
        } else {
            all
        }
        state = state.copy(listings = trimmed)
        saveState(state)
    }

    fun deck(): List<Car> {
        val prefs = state.prefs ?: return emptyList()
        return MatchEngine.deck(state.listings, prefs, state.affinity, tasteW(), swipedIds)
    }

    fun matches(): List<ScoredCar> {
        val prefs = state.prefs ?: return emptyList()
        return MatchEngine.rank(state.listings, prefs, state.affinity, tasteW())
    }

    fun bestMatch(): BestMatch? {
        val prefs = state.prefs ?: return null
        return MatchEngine.bestMatch(state.listings, prefs, state.affinity, tasteW(), state.swipes.size, state.answered)
    }

    fun answerAsk(id: String, patch: (UserPrefs) -> UserPrefs) = answerRefine(id, patch)

    fun scored(car: Car): ScoredCar? =
        state.prefs?.let { MatchEngine.score(car, it, state.affinity, tasteW()) }

    fun car(id: String): Car? = state.listings.firstOrNull { it.id == id }

    fun topBrands(limit: Int = 12): List<String> =
        state.listings.mapNotNull { it.brand }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(limit).map { it.key }

    fun open(route: Route) = stack.add(route)
    fun back() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
}
