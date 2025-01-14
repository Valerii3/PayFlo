package dev.valerii.payflo

import dev.valerii.payflo.storage.SettingsStorage
import platform.Foundation.NSUserDefaults

class IOSSettingsStorage : SettingsStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = userDefaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        userDefaults.setObject(value, key)
    }

    override fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }

    override fun clear() {
        userDefaults.dictionaryRepresentation().keys.forEach {
            userDefaults.removeObjectForKey(it.toString())
        }
    }
}

