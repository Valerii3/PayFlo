package dev.valerii.payflo

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import dev.valerii.payflo.di.appModule
import dev.valerii.payflo.screen.WelcomeScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.context.startKoin


private val koin = startKoin {
    modules(appModule)
}.koin


@Composable
@Preview
fun App() {
    MaterialTheme {
        Navigator(WelcomeScreen())
    }
}