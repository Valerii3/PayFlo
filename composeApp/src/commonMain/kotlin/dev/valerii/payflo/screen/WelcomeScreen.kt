package dev.valerii.payflo.screen

import androidx.compose.material3.Button
import androidx.compose.material.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.valerii.payflo.ioDispatcher
import dev.valerii.payflo.model.UserCredentials
import dev.valerii.payflo.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin

class WelcomeScreen : Screen {
    private val userRepository: UserRepository by lazy { getKoin().get<UserRepository>() }
    @Composable
    override fun Content() {
        var name by remember { mutableStateOf(TextFieldValue()) }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope { ioDispatcher }

        LaunchedEffect(Unit) {
            withContext(ioDispatcher) {
                val credentials = userRepository.getSavedCredentials()
                if (credentials != null) {
                    navigator.replace(MainScreen())
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to PayFlo",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                isError = isError
            )

            if (isError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    if (name.text.isBlank()) {
                        isError = true
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val user = userRepository.createUser(name.text)
                            userRepository.saveCredentials(UserCredentials(user.id))
                            navigator.replace(MainScreen())
                        } catch (e: Exception) {
                            isError = true
                            errorMessage = e.message ?: "Failed to create user"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Get Started")
                }
            }
        }
    }
}
