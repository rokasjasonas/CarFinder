package lt.carfinder.platform

import android.annotation.SuppressLint
import android.content.Context
import java.io.File

@SuppressLint("StaticFieldLeak")
object AndroidPlatform {
    lateinit var context: Context
        private set

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }
}

actual object FileStore {
    private fun file(name: String) = File(AndroidPlatform.context.filesDir, name)
    actual fun read(name: String): String? = file(name).takeIf { it.exists() }?.readText()
    actual fun write(name: String, content: String) {
        val f = file(name)
        val tmp = File(f.parentFile, "$name.tmp")
        tmp.writeText(content)
        tmp.renameTo(f)
    }
}
