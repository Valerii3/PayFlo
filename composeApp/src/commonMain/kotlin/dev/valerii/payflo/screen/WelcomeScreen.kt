package dev.valerii.payflo.screen

import androidx.compose.foundation.background
import androidx.compose.material3.TextField
import androidx.compose.material3.Button
import androidx.compose.material.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.valerii.payflo.models.AppViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class WelcomeScreen : Screen {
    @Composable
    override fun Content() {
        var name by remember { mutableStateOf(TextFieldValue()) }
        var username by remember { mutableStateOf(TextFieldValue()) }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        val navigator = LocalNavigator.currentOrThrow

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

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
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
                    if (name.text.isBlank() || username.text.isBlank()) {
                        isError = true
                        errorMessage = "Please fill in all fields"
                    } else {
                        // TODO: Check username uniqueness
                        // TODO: Save user data
                        navigator.push(RoomSelectionScreen())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Started")
            }
        }
    }
}

// Room Selection Screen
class RoomSelectionScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var showJoinDialog by remember { mutableStateOf(false) }

        var code by remember { mutableStateOf(TextFieldValue()) }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose an Option",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    // TODO: Navigate to create room screen
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text("Create New Room")
            }

            Button(
                onClick = {
                    // TODO: Navigate to join room screen
                    showJoinDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join Room by ID")
            }
        }
        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showJoinDialog = false
                    code = TextFieldValue()
                    isError = false
                },
                title = { Text("Join Room") },
                text = {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = {
                                if (it.text.length <= 6) {
                                    code = it
                                    isError = false
                                }
                            },
                            label = { Text("Enter 6-digit code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = isError,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isError) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            when {
                                code.text.length != 6 -> {
                                    isError = true
                                    errorMessage = "Code must be 6 digits"
                                }
                                !code.text.all { it.isDigit() } -> {
                                    isError = true
                                    errorMessage = "Code must contain only numbers"
                                }
                                else -> {
                                    // TODO: Verify room code
                                    // If valid, navigate to room
                                  //  navigator.push(RoomScreen(code.text))
                                    showJoinDialog = false
                                }
                            }
                        }
                    ) {
                        Text("Join")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showJoinDialog = false
                            code = TextFieldValue()
                            isError = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

class JoinScreen : Screen {
    @Composable
    override fun Content() {
        var code by remember { mutableStateOf(TextFieldValue()) }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        val navigator = LocalNavigator.currentOrThrow

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Join Room",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            if (it.text.length <= 6) {
                                code = it
                                isError = false
                            }
                        },
                        label = { Text("Enter 6-digit code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isError) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                code.text.length != 6 -> {
                                    isError = true
                                    errorMessage = "Code must be 6 digits"
                                }
                                !code.text.all { it.isDigit() } -> {
                                    isError = true
                                    errorMessage = "Code must contain only numbers"
                                }
                                else -> {
                                    // TODO: Verify room code
                                    // If valid, navigate to room
                               //     navigator.push(RoomScreen(code.text))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Join")
                    }
                }
            }
        }
    }
}
/*
@Composable
fun WelcomeScreen(viewModel: AppViewModel, onGetStarted: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Unique Login") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        error?.let { Text(it, color = Color.Red) }

        Button(
            onClick = {
                try {
                    viewModel.setUserName(name)
                    viewModel.setLogin(login)
                    onGetStarted()
                } catch (e: IllegalArgumentException) {
                    error = e.message
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}
*/