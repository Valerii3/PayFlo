package dev.valerii.payflo.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import dev.valerii.payflo.ByteArrayImage
import dev.valerii.payflo.ioDispatcher
import dev.valerii.payflo.picker.ImagePicker
import dev.valerii.payflo.rememberImagePicker
import dev.valerii.payflo.repository.UserRepository
import dev.valerii.payflo.storage.SettingsStorage
import dev.valerii.payflo.viewmodel.ProfileUiState
import dev.valerii.payflo.viewmodel.ProfileViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.io.encoding.Base64

class ProfileScreen : Screen, KoinComponent {
    private val userRepository: UserRepository by inject()
    private val settingsStorage: SettingsStorage by inject()
    /* private val imagePicker by inject<ImagePicker>() */
    private val viewModel by lazy { ProfileViewModel(userRepository, settingsStorage/*, imagePicker*/) }

    @Composable
    override fun Content() {
        val uiState by viewModel.uiState.collectAsState()
        var isEditingName by remember { mutableStateOf(false) }
        var editedName by remember { mutableStateOf("") }

        //val scope = rememberCoroutineScope { ioDispatcher }

        val pickImage = rememberImagePicker { imageBytes ->
            viewModel.updateProfilePicture(imageBytes)
        }


        LaunchedEffect(Unit) {
            withContext(ioDispatcher) {
                viewModel.loadProfile()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is ProfileUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ProfileUiState.Success -> {
                    // Profile Image Section
                    Box(
                        modifier = Modifier.padding(top = 32.dp)
                    ) {
                        if (state.user.profilePicture != null) {
                            val imageData = state.user.profilePicture!!.decodeBase64Bytes()
                            ByteArrayImage(
                                imageBytes = imageData,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                        } else {
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
                                    contentDescription = "Change Profile Picture",
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
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                        Button(
                            onClick = {
                                viewModel.updateName(editedName)
                                isEditingName = false
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Save")
                        }
                    } else {
                        Text(
                            text = state.user.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        TextButton(
                            onClick = {
                                editedName = state.user.name
                                isEditingName = true
                            }
                        ) {
                            Text("Edit Name")
                        }
                    }
                }
            }
        }
    }
}

