package dev.valerii.payflo.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.valerii.payflo.ioDispatcher
import dev.valerii.payflo.model.Group
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.viewmodel.GroupDetailUiState
import dev.valerii.payflo.viewmodel.GroupDetailViewModel
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import dev.valerii.payflo.model.Expense
import dev.valerii.payflo.model.User
import dev.valerii.payflo.repository.UserRepository
import dev.valerii.payflo.storage.SettingsStorage

class GroupDetailScreen(private val group: Group) : Screen, KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val userRepository: UserRepository by inject()
    private val viewModel by lazy {
        GroupDetailViewModel(
            groupRepository,
            userRepository,
            group.id
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsState()


        LaunchedEffect(Unit) {
            withContext(ioDispatcher) {
                viewModel.loadGroup()
            }
        }

        Scaffold(
            topBar = {
                when (val state = uiState) {
                    is GroupDetailUiState.Success -> {
                        TopAppBar(
                            title = { Text(state.group.name) },
                            navigationIcon = {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(Icons.Default.ArrowBack, "Go back")
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { navigator.push(GroupSettingsScreen(state.group)) }
                                ) {
                                    Icon(Icons.Default.Settings, "Group Settings")
                                }
                            }
                        )
                    }

                    else -> {
                        TopAppBar(
                            title = { Text("Loading...") },
                            navigationIcon = {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(Icons.Default.ArrowBack, "Go back")
                                }
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigator.push(AddExpenseScreen(group)) }
                ) {
                    Icon(Icons.Default.Add, "Add Bill")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    is GroupDetailUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    is GroupDetailUiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    is GroupDetailUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.group.expenses) { expense ->
                                ExpenseCard(
                                    expense = expense,
                                    currentUserId = state.userId,
                                    participants = state.group.participants,
                                    onClick = {
                                        // Show expense details dialog or navigate to details screen
                                        navigator.push(
                                            ExpenseDetailScreen(
                                                expense = expense,
                                                participants = state.group.participants
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    currentUserId: String,
    participants: List<User>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = expense.name,
                    style = MaterialTheme.typography.titleMedium
                )

                val amount = if (expense.paidById == currentUserId) {
                    "+₴${expense.amount}"
                } else if (expense.participantIds.contains(currentUserId)) {
                    "-₴${expense.amount / expense.participantIds.size}"
                } else {
                    "₴0.0"
                }

                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (amount.startsWith("+"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Show who paid
            val paidByUser = participants.find { it.id == expense.paidById }
            Text(
                text = "Paid by ${paidByUser?.name ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Show share amount
            val shareAmount = expense.amount / expense.participantIds.size
            Text(
                text = "${expense.participantIds.size} participants • ₴$shareAmount each",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}