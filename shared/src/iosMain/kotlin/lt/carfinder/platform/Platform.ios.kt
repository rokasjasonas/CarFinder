package lt.carfinder.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
actual object FileStore {
    private fun path(name: String): String {
        val dir = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).first()
        return (dir as platform.Foundation.NSURL).path + "/" + name
    }

    actual fun read(name: String): String? =
        NSString.stringWithContentsOfFile(path(name), NSUTF8StringEncoding, null)

    @Suppress("CAST_NEVER_SUCCEEDS")
    actual fun write(name: String, content: String) {
        (content as NSString).writeToFile(path(name), true, NSUTF8StringEncoding, null)
    }
}

actual fun openInBrowser(url: String) {
    NSURL.URLWithString(url)?.let {
        platform.UIKit.UIApplication.sharedApplication.openURL(it, mapOf<Any?, Any?>()) { }
    }
}
