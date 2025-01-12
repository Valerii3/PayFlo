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


data class Friend(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val lastSeen: String,
    val isOnline: Boolean = false
)

class CreateRoomScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var searchQuery by remember { mutableStateOf("") }
        var selectedFriends by remember { mutableStateOf(setOf<String>()) }

        // Sample friends data (you would get this from your data source)
        val allFriends = remember {
            listOf(
                Friend("1", "Artin", null, "last seen recently"),
                Friend("2", "Bering", null, "last seen yesterday at 12:42"),
                Friend("3", "Boat Paleokk", null, "last seen 4 hours ago"),
                Friend("4", "Germania Nooksks", null, "last seen 05.12.24"),
                Friend("5", "Gleb Ingman", null, "online", true),
                Friend("6", "Interview", null, "last seen 15.04.24")
            )
        }

        // Filter friends based on search query
        val filteredFriends = allFriends.filter {
            it.name.lowercase().contains(searchQuery.lowercase())
        }

        // Group friends by first letter
        val groupedFriends = filteredFriends
            .groupBy { it.name.first().uppercase() }
            .toList()  // converts to list of pairs
            .sortedBy { it.first }  // sorts by the letter
            .toMap()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("New Group")
                            Text(
                                "${selectedFriends.size}/${allFriends.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                // TODO: Navigate to step 2 with selected friends
                            },
                            enabled = selectedFriends.isNotEmpty()
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

                // Friends list
                LazyColumn {
                    groupedFriends.forEach { (letter, friends) ->
                        // Letter header
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

                        // Friends under this letter
                        items(friends) { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Checkbox
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

                                // Friend info
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = friend.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = if (friend.isOnline) "online" else friend.lastSeen,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (friend.isOnline)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
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