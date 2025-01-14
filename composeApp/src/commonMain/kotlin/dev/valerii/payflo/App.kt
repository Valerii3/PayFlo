package dev.valerii.payflo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import dev.valerii.payflo.di.appModule
import dev.valerii.payflo.screen.WelcomeScreen
import kotlinx.coroutines.CoroutineExceptionHandler
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.context.startKoin

import payflo.composeapp.generated.resources.Res
import payflo.composeapp.generated.resources.compose_multiplatform

private val koin = startKoin {
    modules(appModule)
}.koin

@Composable
@Preview
fun App() {
    DisposableEffect(Unit) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            println("Uncaught Kotlin coroutine exception: ${throwable.message}")
            throwable.printStackTrace()
        }

        onDispose {

        }
    }

    MaterialTheme {
        Navigator(WelcomeScreen())
    }
}