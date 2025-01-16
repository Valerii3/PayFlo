package dev.valerii.payflo.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.valerii.payflo.image.rememberImagePicker
import dev.valerii.payflo.model.Group

import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.storage.SettingsStorage
import dev.valerii.payflo.viewmodel.AddExpenseUiState
import dev.valerii.payflo.viewmodel.AddExpenseViewModel
import io.ktor.util.encodeBase64
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AddExpenseScreen(private val group: Group) : Screen, KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val settingsStorage: SettingsStorage by inject()
    private val viewModel by lazy { AddExpenseViewModel(groupRepository, settingsStorage, group) }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsState()

        var expenseName by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var selectedParticipants by remember { mutableStateOf(setOf<String>()) }
        var billImageBase64 by remember { mutableStateOf<String?>(null) }

        val pickImage = rememberImagePicker { imageBytes ->
            billImageBase64 = imageBytes.encodeBase64()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Expense") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, "Go back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = expenseName,
                    onValueChange = { expenseName = it },
                    label = { Text("Expense Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Manual amount input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "  OR  ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { pickImage() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Attach Bill",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (billImageBase64 != null) "Bill Attached" else "Attach Bill")
                }


                Spacer(modifier = Modifier.height(16.dp))

                // Select All checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Select All")
                    Checkbox(
                        checked = selectedParticipants.size == group.participants.size,
                        onCheckedChange = { checked ->
                            selectedParticipants = if (checked) {
                                group.participants.map { it.id }.toSet()
                            } else {
                                emptySet()
                            }
                        }
                    )
                }

                LazyColumn {
                    items(group.participants) { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(participant.name)
                            Checkbox(
                                checked = participant.id in selectedParticipants,
                                onCheckedChange = { checked ->
                                    selectedParticipants = if (checked) {
                                        selectedParticipants + participant.id
                                    } else {
                                        selectedParticipants - participant.id
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Show split amount if any participants selected
                if (selectedParticipants.isNotEmpty()) {
                    amount.toDoubleOrNull()?.let { totalAmount ->
                        val splitAmount = totalAmount / selectedParticipants.size
                        Text(
                            "Each person will pay: ${(splitAmount * 100.0).toInt() / 100.0}",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.addExpense(
                            name = expenseName,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            participantIds = selectedParticipants.toList(),
                            billImage = billImageBase64
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = expenseName.isNotBlank() &&
                            selectedParticipants.isNotEmpty() &&
                            (amount.isNotBlank() || billImageBase64 != null)
                ) {
                    Text("Add Expense")
                }
            }
        }

        LaunchedEffect(uiState) {
            when (uiState) {
                is AddExpenseUiState.Success -> navigator.pop()
                is AddExpenseUiState.Error -> {
                    // Show error message
                }
                else -> {}
            }
        }
    }
}
