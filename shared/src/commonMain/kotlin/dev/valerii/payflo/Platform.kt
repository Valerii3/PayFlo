package dev.valerii.payflo

import dev.valerii.payflo.storage.SettingsStorage
import kotlinx.coroutines.CoroutineDispatcher

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getSettingsStorage(): SettingsStorage

expect val ioDispatcher: CoroutineDispatcher