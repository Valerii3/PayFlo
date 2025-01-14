package dev.valerii.payflo.viewmodel

import dev.valerii.payflo.model.User
import dev.valerii.payflo.repository.ContactRepository
import dev.valerii.payflo.storage.SettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CreateRoomViewModel(
    private val contactRepository: ContactRepository,
    private val settingsStorage: SettingsStorage
) {
    private val _uiState = MutableStateFlow<CreateRoomUiState>(CreateRoomUiState.Loading)
    val uiState: StateFlow<CreateRoomUiState> = _uiState

    fun loadFriends() {
        _uiState.value = CreateRoomUiState.Loading
        scope.launch {
            try {
                val userId = settingsStorage.getString("user_id") ?: run {
                    _uiState.value = CreateRoomUiState.Error("User ID not found")
                    return@launch
                }

                val friends = contactRepository.getFriends(userId)
                _uiState.value = CreateRoomUiState.Success(friends)
            } catch (e: Exception) {
                _uiState.value = CreateRoomUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

sealed class CreateRoomUiState {
    object Loading : CreateRoomUiState()
    data class Success(val friends: List<User>) : CreateRoomUiState()
    data class Error(val message: String) : CreateRoomUiState()
}