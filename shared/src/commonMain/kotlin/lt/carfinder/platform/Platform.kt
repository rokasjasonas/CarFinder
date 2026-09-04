package lt.carfinder.platform

expect object FileStore {
    fun read(name: String): String?
    fun write(name: String, content: String)
}

expect fun openInBrowser(url: String)
