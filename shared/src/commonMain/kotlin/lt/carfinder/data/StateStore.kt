package lt.carfinder.data

import kotlinx.serialization.json.Json
import lt.carfinder.model.AppState
import lt.carfinder.platform.FileStore

private const val FILE = "state.json"
private val json = Json { ignoreUnknownKeys = true }

fun loadState(): AppState =
    FileStore.read(FILE)
        ?.let { raw -> runCatching { json.decodeFromString<AppState>(raw) }.getOrNull() }
        ?: AppState()

fun saveState(state: AppState) {
    runCatching { FileStore.write(FILE, json.encodeToString(state)) }
}
