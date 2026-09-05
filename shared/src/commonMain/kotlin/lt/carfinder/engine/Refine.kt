package lt.carfinder.engine

object Refine {
    const val TOTAL = 10

    val INITIAL = listOf("budget", "usage", "fuel", "gearbox", "priority")
    val EXTRA = listOf("year", "bodies", "brands", "power", "mileage")

    fun sharpness(answered: Set<String>): Int {
        val n = (INITIAL + EXTRA).count { it in answered }
        return (n * 100 / TOTAL).coerceIn(0, 100)
    }
}
