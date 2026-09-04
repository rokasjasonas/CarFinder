package lt.carfinder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import lt.carfinder.data.Catalog
import lt.carfinder.data.loadState
import lt.carfinder.data.saveState
import lt.carfinder.engine.MatchEngine
import lt.carfinder.engine.ScoredCar
import lt.carfinder.model.Car
import lt.carfinder.model.Swipe
import lt.carfinder.model.UserPrefs

sealed interface Tab {
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

    var tab by mutableStateOf<Tab>(Tab.Discover)
    val stack = mutableStateListOf<Route>()

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
        val car = Catalog.car(carId) ?: return
        state = state.copy(
            swipes = state.swipes + Swipe(carId, liked),
            affinity = MatchEngine.learn(state.affinity, car, liked),
        )
        saveState(state)
    }

    fun deck(): List<Car> {
        val prefs = state.prefs ?: return emptyList()
        return MatchEngine.deck(Catalog.cars, prefs, state.affinity, tasteW(), swipedIds)
    }

    fun matches(): List<ScoredCar> {
        val prefs = state.prefs ?: return emptyList()
        return MatchEngine.rank(Catalog.cars, prefs, state.affinity, tasteW())
    }

    fun scored(car: Car): ScoredCar? =
        state.prefs?.let { MatchEngine.score(car, it, state.affinity, tasteW()) }

    fun car(id: String): Car? = Catalog.car(id)

    fun open(route: Route) = stack.add(route)
    fun back() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
}
