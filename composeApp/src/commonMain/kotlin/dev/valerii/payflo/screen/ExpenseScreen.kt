package dev.valerii.payflo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.valerii.payflo.elements.CreateGroupDialog
import dev.valerii.payflo.model.Group
import dev.valerii.payflo.model.User
import dev.valerii.payflo.repository.ContactRepository
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.repository.UserRepository
import dev.valerii.payflo.storage.SettingsStorage
import dev.valerii.payflo.viewmodel.AddExpenseUiState
import dev.valerii.payflo.viewmodel.AddExpenseViewModel
import dev.valerii.payflo.viewmodel.CreateRoomUiState
import dev.valerii.payflo.viewmodel.CreateRoomViewModel
import dev.valerii.payflo.viewmodel.GroupCreationState
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AddExpenseScreen(private val group: Group) : Screen, KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val viewModel by lazy { AddExpenseViewModel(groupRepository, group) }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsState()

        var expenseName by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var selectedParticipants by remember { mutableStateOf(setOf<String>()) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Expense") },
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
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = expenseName,
                    onValueChange = { expenseName = it },
                    label = { Text("Expense Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

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
                        amount.toDoubleOrNull()?.let { amountValue ->
                            viewModel.addExpense(
                                name = expenseName,
                                amount = amountValue,
                                participantIds = selectedParticipants.toList()
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = expenseName.isNotBlank() &&
                            amount.isNotBlank() &&
                            selectedParticipants.isNotEmpty()
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

