package dev.valerii.payflo

import dev.valerii.payflo.storage.SettingsStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getSettingsStorage(): SettingsStorage {
    throw UnsupportedOperationException("getSettingsStorage is not supported on JVM")
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO