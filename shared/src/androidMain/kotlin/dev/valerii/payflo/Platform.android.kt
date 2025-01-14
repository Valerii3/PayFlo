package dev.valerii.payflo

import android.os.Build
import dev.valerii.payflo.storage.SettingsStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

// Then use it in getSettingsStorage
actual fun getSettingsStorage(): SettingsStorage {
    return AndroidSettingsStorage(AndroidApp.context)
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO