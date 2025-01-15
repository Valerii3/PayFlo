package dev.valerii.payflo.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.valerii.payflo.ByteArrayImage
import dev.valerii.payflo.ioDispatcher
import dev.valerii.payflo.model.Group
import dev.valerii.payflo.model.User
import dev.valerii.payflo.rememberImagePicker
import dev.valerii.payflo.repository.ContactRepository
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.repository.UserRepository
import dev.valerii.payflo.storage.SettingsStorage
import dev.valerii.payflo.viewmodel.GroupSettingsUiState
import dev.valerii.payflo.viewmodel.GroupSettingsViewModel
import dev.valerii.payflo.viewmodel.ProfileUiState
import dev.valerii.payflo.viewmodel.ProfileViewModel
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class GroupSettingsScreen(private val group: Group) : Screen, KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val contactRepository: ContactRepository by inject()
    private val settingsStorage: SettingsStorage by inject()
    private val viewModel by lazy {
        GroupSettingsViewModel(
            groupRepository,
            contactRepository,
            settingsStorage,
            group.id
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsState()
        var isEditingName by remember { mutableStateOf(false) }
        var editedName by remember { mutableStateOf(group.name) }

        val pickImage = rememberImagePicker { imageBytes ->
            viewModel.updateGroupPicture(imageBytes)
        }

        LaunchedEffect(Unit) {
            withContext(ioDispatcher) {
                viewModel.loadGroup()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Group Settings") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, "Go back")
                        }
                    }
                )
            }
        ) { padding ->
            when (val state = uiState) {
                is GroupSettingsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is GroupSettingsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is GroupSettingsUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Group Image Section
                        Box(
                            modifier = Modifier.padding(top = 32.dp)
                        ) {
                            if (state.group.photo != null) {
                                val imageData = state.group.photo!!.decodeBase64Bytes()
                                print("IMAGE:" + imageData)
                                ByteArrayImage(
                                    imageBytes = imageData,
                                    contentDescription = "Group Picture",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                println("NO IMAGE")
                                Surface(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(24.dp)
                                            .fillMaxSize(),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            IconButton(
                                onClick = { pickImage() },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-8).dp, y = (-8).dp)
                                    .size(36.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Change Group Picture",
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Name Section
                        if (isEditingName) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Group Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                            Button(
                                onClick = {
                                    viewModel.updateGroupName(editedName)
                                    isEditingName = false
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Save")
                            }
                        } else {
                            Text(
                                text = state.group.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = "${state.group.participants.size} members",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = {
                                    editedName = state.group.name
                                    isEditingName = true
                                }
                            ) {
                                Text("Edit Name")
                            }
                        }

                        // Pincode Section
                        Text(
                            text = "Pincode: ${state.group.inviteCode}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Participants List
                        Text(
                            text = "Participants",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.group.participants) { participant ->
                                UserItem(participant)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun UserItem(participant: User) {
        val currentUserId = remember { settingsStorage.getString("user_id") }
        val friends by viewModel.friends.collectAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = participant.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (participant.id != currentUserId) {
                if (friends.contains(participant.id)) {
                    Text(
                        text = "In Friends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    TextButton(
                        onClick = { viewModel.addFriend(participant.id) }
                    ) {
                        Text("Add Friend")
                    }
                }
            }
        }
    }

}

