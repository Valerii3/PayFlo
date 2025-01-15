package dev.valerii.payflo.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.storage.SettingsStorage
import dev.valerii.payflo.viewmodel.GroupChoiceViewModel
import dev.valerii.payflo.viewmodel.JoinGroupUiState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GroupChoiceScreen : Screen, KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val settingsStorage: SettingsStorage by inject()
    private val viewModel by lazy { GroupChoiceViewModel(groupRepository, settingsStorage) }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var showJoinDialog by remember { mutableStateOf(false) }
        var pincode by remember { mutableStateOf("") }
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(uiState) {
            when (val state = uiState) {
                is JoinGroupUiState.Success -> {
                    showJoinDialog = false
                    navigator.pop() // Go back to groups list
                    navigator.push(GroupDetailScreen(state.group)) // Navigate to group details
                }
                else -> Unit
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("New Group") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, "Go back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { navigator.push(CreateRoomScreen()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Create New Group")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showJoinDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Join by Pincode")
                }
            }
        }

        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showJoinDialog = false
                    pincode = ""
                    viewModel.resetState()
                },
                title = { Text("Join Group") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pincode,
                            onValueChange = {
                                pincode = it
                                if (uiState is JoinGroupUiState.Error) {
                                    viewModel.resetState()
                                }
                            },
                            label = { Text("Enter Pincode") },
                            singleLine = true,
                            isError = uiState is JoinGroupUiState.Error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                        when (val state = uiState) {
                            is JoinGroupUiState.Error -> {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            is JoinGroupUiState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }
                            else -> Unit
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.joinGroup(pincode) },
                        enabled = uiState !is JoinGroupUiState.Loading
                    ) {
                        Text("Join")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showJoinDialog = false
                            pincode = ""
                            viewModel.resetState()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}