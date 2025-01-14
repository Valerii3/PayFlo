package dev.valerii.payflo.viewmodel

import dev.valerii.payflo.model.Group
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.storage.SettingsStorage
import dev.valerii.payflo.repository.UserRepository
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupViewModel(
    private val groupRepository: GroupRepository,
    private val settingsStorage: SettingsStorage
) {
    private val _uiState = MutableStateFlow<GroupsUiState>(GroupsUiState.Loading)
    val uiState: StateFlow<GroupsUiState> = _uiState

    fun loadGroups() {
        _uiState.value = GroupsUiState.Loading
        scope.launch {
            try {
                val userId = settingsStorage.getString("user_id") ?: run {
                    _uiState.value = GroupsUiState.Error("User ID not found")
                    return@launch
                }

                val groups = groupRepository.getGroupsForUser(userId)
                _uiState.value = GroupsUiState.Success(groups)
            } catch (e: Exception) {
                _uiState.value = GroupsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

sealed class GroupsUiState {
    object Loading : GroupsUiState()
    data class Success(val groups: List<Group>) : GroupsUiState()
    data class Error(val message: String) : GroupsUiState()
}