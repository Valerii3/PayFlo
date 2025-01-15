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
import dev.valerii.payflo.model.Expense
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

class ExpenseDetailScreen(
    private val expense: Expense,
    private val participants: List<User>
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(expense.name) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, "Go back")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Expense total
                item {
                    Text(
                        "Total Amount",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "₴${expense.amount}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Paid by
                item {
                    val paidByUser = participants.find { it.id == expense.paidById }
                    Text(
                        "Paid by",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        paidByUser?.name ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Participants
                item {
                    Text(
                        "Participants",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // List each participant and their share
                val shareAmount = expense.amount / expense.participantIds.size
                items(participants.filter { it.id in expense.participantIds }) { participant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(participant.name)
                        Text(
                            "₴$shareAmount",
                            color = if (participant.id == expense.paidById)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}