package dev.valerii.payflo.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.valerii.payflo.elements.GroupCard
import dev.valerii.payflo.model.Group
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.storage.SettingsStorage
import dev.valerii.payflo.viewmodel.GroupViewModel
import dev.valerii.payflo.viewmodel.GroupsUiState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject



@OptIn(ExperimentalMaterial3Api::class)
class GroupsScreen : Screen, KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val settingsStorage: SettingsStorage by inject()
    private val viewModel by lazy { GroupViewModel(groupRepository, settingsStorage) }


    @Composable
    override fun Content() {
        val uiState by viewModel.uiState.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            viewModel.loadGroups()
        }


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Groups") },
                    actions = {
                        IconButton(onClick = { navigator.push(GroupChoiceScreen()) }) {
                            Icon(Icons.Default.Add, "Create New Group")
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
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search groups...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    singleLine = true
                )

                when (val state = uiState) {
                    is GroupsUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    is GroupsUiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    is GroupsUiState.Success -> {
                        val filteredGroups = state.groups.filter {
                            it.name.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredGroups) { group ->
                                GroupCard(group = group) {
                                    // Navigate to group details
                                    navigator.push(GroupDetailScreen(group))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
