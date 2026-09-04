package lt.carfinder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import lt.carfinder.data.loadState
import lt.carfinder.data.saveState
import lt.carfinder.engine.MatchEngine
import lt.carfinder.engine.ScoredCar
import lt.carfinder.model.Car
import lt.carfinder.model.Swipe
import lt.carfinder.model.UserPrefs
import lt.carfinder.sites.Site
import lt.carfinder.sites.Sites

sealed interface Tab {
    data object Browse : Tab
    data object Discover : Tab
    data object Matches : Tab
    data object Profile : Tab
}

sealed interface Route {
    data class CarDetail(val carId: String) : Route
}

class AppViewModel : ViewModel() {

    var state by mutableStateOf(loadState())
        private set

    var tab by mutableStateOf<Tab>(Tab.Browse)
    val stack = mutableStateListOf<Route>()

    /** URL the Browse WebView is currently on; also drives the site switcher. */
    var browseUrl by mutableStateOf(Sites.AUTOPLIUS.defaultSearch)
    val browseSite: Site get() = Sites.detect(browseUrl)

    val hasPrefs: Boolean get() = state.prefs != null
    val swipedIds: Set<String> get() = state.swipes.map { it.carId }.toSet()

    private fun tasteW(): Float = MatchEngine.tasteWeight(state.swipes.size)

    fun completeQuiz(prefs: UserPrefs) {
        state = state.copy(prefs = prefs)
        saveState(state)
    }

    fun retakeQuiz() {
        state = state.copy(prefs = null)
        saveState(state)
    }

    fun resetTaste() {
        state = state.copy(affinity = emptyMap(), swipes = emptyList())
        saveState(state)
    }

    fun swipe(carId: String, liked: Boolean) {
        val car = car(carId) ?: return
        state = state.copy(
            swipes = state.swipes + Swipe(carId, liked),
            affinity = MatchEngine.learn(state.affinity, car, liked),
        )
        saveState(state)
    }

    /** Payload from either WebView (Browse or the hidden gallery prefetcher). */
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

    fun scored(car: Car): ScoredCar? =
        state.prefs?.let { MatchEngine.score(car, it, state.affinity, tasteW()) }

    fun car(id: String): Car? = state.listings.firstOrNull { it.id == id }

    /** Search-card harvests carry one thumbnail; load the listing page once to get the full gallery. */
    fun needsGallery(car: Car): Boolean = car.photos.size <= 1 && !galleryPrefetched.contains(car.id) && galleryPrefetched.add(car.id)

    private val galleryPrefetched = mutableSetOf<String>()

    fun open(route: Route) = stack.add(route)
    fun back() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
}
