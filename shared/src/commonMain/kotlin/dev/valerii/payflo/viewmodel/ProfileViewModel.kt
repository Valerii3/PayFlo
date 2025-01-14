package dev.valerii.payflo.viewmodel

import dev.valerii.payflo.model.User
import dev.valerii.payflo.repository.UserRepository
import dev.valerii.payflo.storage.SettingsStorage
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val settingsStorage: SettingsStorage,
) {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun loadProfile() {
        _uiState.value = ProfileUiState.Loading
        scope.launch {
            try {
                val userId = settingsStorage.getString("user_id") ?: run {
                    _uiState.value = ProfileUiState.Error("User ID not found in settings")
                    return@launch
                }
                userRepository.getUser(userId)?.let { user ->
                    _uiState.value = ProfileUiState.Success(user)
                } ?: run {
                    _uiState.value = ProfileUiState.Error("User not found")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateName(newName: String) {
        scope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    val updatedUser = currentState.user.copy(name = newName)
                    val result = userRepository.updateUser(updatedUser)
                    _uiState.value = ProfileUiState.Success(result)
                }
            } catch (e: Exception) {
                // Handle error but keep current state
            }
        }
    }

    fun updateProfilePicture(imageBytes: ByteArray) {
        scope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is ProfileUiState.Success) {
                    // Convert ByteArray to Base64 string
                    val base64Image = imageBytes.encodeBase64()
                    val updatedUser = currentState.user.copy(profilePicture = base64Image)
                    val result = userRepository.updateUser(updatedUser)
                    _uiState.value = ProfileUiState.Success(result)
                }
            } catch (e: Exception) {
                // Handle error but keep current state
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}