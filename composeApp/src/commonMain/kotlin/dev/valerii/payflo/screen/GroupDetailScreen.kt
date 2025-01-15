package dev.valerii.payflo.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

class GroupDetailScreen(private val group: Group) : Screen, KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val viewModel by lazy { GroupDetailViewModel(groupRepository, group.id) }

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
                        // Your group content here using state.group
                        Text("Group Details Content", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}