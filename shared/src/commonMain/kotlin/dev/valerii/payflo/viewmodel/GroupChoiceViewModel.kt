package dev.valerii.payflo.viewmodel

import dev.valerii.payflo.model.Group
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.storage.SettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class JoinGroupUiState {
    object Initial : JoinGroupUiState()
    object Loading : JoinGroupUiState()
    data class Success(val group: Group) : JoinGroupUiState()
    data class Error(val message: String) : JoinGroupUiState()
}

class GroupChoiceViewModel(
    private val groupRepository: GroupRepository,
    private val settingsStorage: SettingsStorage
) {
    private val _uiState = MutableStateFlow<JoinGroupUiState>(JoinGroupUiState.Initial)
    val uiState: StateFlow<JoinGroupUiState> = _uiState

    fun joinGroup(pincode: String) {
        if (pincode.isBlank()) {
            _uiState.value = JoinGroupUiState.Error("Please enter a pincode")
            return
        }

        _uiState.value = JoinGroupUiState.Loading
        scope.launch {
            try {
                val userId = settingsStorage.getString("user_id") ?: run {
                    _uiState.value = JoinGroupUiState.Error("User ID not found")
                    return@launch
                }

                groupRepository.joinGroup(pincode, userId)
                    .onSuccess { group ->
                        _uiState.value = JoinGroupUiState.Success(group)
                    }
                    .onFailure { e ->
                        _uiState.value = JoinGroupUiState.Error(e.message ?: "Failed to join group")
                    }
            } catch (e: Exception) {
                _uiState.value = JoinGroupUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _uiState.value = JoinGroupUiState.Initial
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}