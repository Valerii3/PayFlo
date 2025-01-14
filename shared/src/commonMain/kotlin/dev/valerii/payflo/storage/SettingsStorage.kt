package dev.valerii.payflo.storage

interface SettingsStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()
}

