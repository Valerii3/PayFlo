package dev.valerii.payflo.di

import dev.valerii.payflo.getSettingsStorage
import dev.valerii.payflo.repository.ContactRepository
import dev.valerii.payflo.repository.ContactRepositoryImpl
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.repository.GroupRepositoryImpl
import org.koin.dsl.module
import kotlinx.serialization.json.Json
import dev.valerii.payflo.repository.UserRepository
import dev.valerii.payflo.repository.UserRepositoryImpl
import dev.valerii.payflo.storage.SettingsStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

val appModule = module {
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

    single<SettingsStorage> {
        getSettingsStorage()
    }

    single<UserRepository> {
        UserRepositoryImpl(
            httpClient = get(),
            settingsStorage = get()
        )
    }

    single<GroupRepository> {
        GroupRepositoryImpl(
            httpClient = get()
        )
    }

    single<ContactRepository> { ContactRepositoryImpl(get()) }
}