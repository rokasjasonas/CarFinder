package lt.carfinder.util

fun Int.groupThousands(): String {
    val s = toString()
    val out = StringBuilder()
    for ((i, c) in s.withIndex()) {
        if (i > 0 && (s.length - i) % 3 == 0) out.append(' ')
        out.append(c)
    }
    return out.toString()
}

fun Int.asPrice(): String = "€${groupThousands()}"
