package dev.valerii.payflo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.valerii.payflo.elements.CreateGroupDialog
import dev.valerii.payflo.repository.ContactRepository
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.repository.UserRepository
import dev.valerii.payflo.storage.SettingsStorage
import dev.valerii.payflo.viewmodel.CreateRoomUiState
import dev.valerii.payflo.viewmodel.CreateRoomViewModel
import dev.valerii.payflo.viewmodel.GroupCreationState
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class CreateRoomScreen : Screen, KoinComponent {
    private val contactRepository: ContactRepository by inject()
    private val settingsStorage: SettingsStorage by inject()
    private val groupRepository: GroupRepository by inject()
    private val viewModel = CreateRoomViewModel(contactRepository, settingsStorage, groupRepository)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsState()
        val groupCreationState by viewModel.groupCreationState.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        var selectedFriends by remember { mutableStateOf(setOf<String>()) }
        var showCreateGroupDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.loadFriends()
        }

        LaunchedEffect(groupCreationState) {
            when (groupCreationState) {
                is GroupCreationState.Success -> {
                    navigator.push(MainScreen())
                }
                is GroupCreationState.Error -> {
                    // Handle error
                }
                else -> { /* Loading or Idle state */ }
            }
        }

        if (showCreateGroupDialog) {
            CreateGroupDialog(
                selectedFriendIds = selectedFriends,
                onDismiss = { showCreateGroupDialog = false },
                onConfirm = { groupName ->
                    viewModel.createGroup(groupName, selectedFriends)
                   // navigator.push(MainScreen())
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("New Group")
                            when (val state = uiState) {
                                is CreateRoomUiState.Success -> {
                                    Text(
                                        "${selectedFriends.size}/${state.friends.size}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                else -> Text(
                                    "0/0",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { showCreateGroupDialog = true },
                            enabled = true
                        ) {
                            Text("Next")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Who would you like to add?") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    singleLine = true
                )

                when (val state = uiState) {
                    is CreateRoomUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    is CreateRoomUiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    is CreateRoomUiState.Success -> {
                        val filteredFriends = state.friends.filter {
                            it.name.contains(searchQuery, ignoreCase = true)
                        }

                        val groupedFriends = filteredFriends
                            .groupBy { it.name.first().uppercase() }
                            .toList()
                            .sortedBy { it.first }
                            .toMap()

                        LazyColumn {
                            groupedFriends.forEach { (letter, friends) ->
                                item {
                                    Text(
                                        text = letter,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                items(friends) { friend ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedFriends.contains(friend.id),
                                            onCheckedChange = { checked ->
                                                selectedFriends = if (checked) {
                                                    selectedFriends + friend.id
                                                } else {
                                                    selectedFriends - friend.id
                                                }
                                            }
                                        )

                                        Text(
                                            text = friend.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}