package dev.valerii.payflo.di

import dev.valerii.payflo.getSettingsStorage
import org.koin.dsl.module
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import dev.valerii.payflo.repository.UserRepository
import dev.valerii.payflo.repository.UserRepositoryImpl
import dev.valerii.payflo.storage.SettingsStorage

val appModule = module {
    // Create HttpClient
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
        }
    }

    // Platform-specific SettingsStorage (implement in respective source sets)
    single<SettingsStorage> {
        getSettingsStorage()
    }

    // UserRepository implementation
    single<UserRepository> {
        UserRepositoryImpl(
            httpClient = get(),
            settingsStorage = get()
        )
    }
}


