package dev.valerii.payflo

import dev.valerii.payflo.storage.SettingsStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getSettingsStorage(): SettingsStorage {
    return IOSSettingsStorage()
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default