package dev.valerii.payflo.viewmodel

import dev.valerii.payflo.model.Group
import dev.valerii.payflo.repository.GroupRepository
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupSettingsViewModel(
    private val groupRepository: GroupRepository,
    private val groupId: String
) {
    private val _uiState = MutableStateFlow<GroupSettingsUiState>(GroupSettingsUiState.Loading)
    val uiState: StateFlow<GroupSettingsUiState> = _uiState

    fun loadGroup() {
        _uiState.value = GroupSettingsUiState.Loading
        scope.launch {
            try {
                println("LOADING")
                groupRepository.getGroup(groupId)?.let { group ->
                    print(group)
                    _uiState.value = GroupSettingsUiState.Success(group)
                } ?: run {
                    _uiState.value = GroupSettingsUiState.Error("Group not found")
                }
            } catch (e: Exception) {
                _uiState.value = GroupSettingsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateGroupName(newName: String) {
        scope.launch {
            try {
                val result = groupRepository.updateGroup(groupId, name = newName, photo = null)
                println("Repository returned updated group: ${result.name}")
                _uiState.value = GroupSettingsUiState.Success(result)
            } catch (e: Exception) {
                println("Error updating name: ${e.message}") // Debug log
                e.printStackTrace() // Print full stack trace
            }
        }
    }

    fun updateGroupPicture(imageBytes: ByteArray) {
        scope.launch {
            try {
                val base64Image = imageBytes.encodeBase64()
                val result = groupRepository.updateGroup(groupId, name = null, photo = base64Image)
                _uiState.value = GroupSettingsUiState.Success(result)
            } catch (e: Exception) {
                // Handle error but keep current state
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

sealed class GroupSettingsUiState {
    object Loading : GroupSettingsUiState()
    data class Success(val group: Group) : GroupSettingsUiState()
    data class Error(val message: String) : GroupSettingsUiState()
}
