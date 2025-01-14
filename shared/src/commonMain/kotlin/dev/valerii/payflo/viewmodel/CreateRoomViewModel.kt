package dev.valerii.payflo.viewmodel

import dev.valerii.payflo.model.Group
import dev.valerii.payflo.model.User
import dev.valerii.payflo.repository.ContactRepository
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.storage.SettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class GroupCreationState {
    object Idle : GroupCreationState()
    object Loading : GroupCreationState()
    data class Success(val group: Group) : GroupCreationState()
    data class Error(val message: String) : GroupCreationState()
}

sealed class CreateRoomUiState {
    object Loading : CreateRoomUiState()
    data class Success(val friends: List<User>) : CreateRoomUiState()
    data class Error(val message: String) : CreateRoomUiState()
}

class CreateRoomViewModel(
    private val contactRepository: ContactRepository,
    private val settingsStorage: SettingsStorage,
    private val groupRepository: GroupRepository,
) {
    private val _uiState = MutableStateFlow<CreateRoomUiState>(CreateRoomUiState.Loading)
    val uiState: StateFlow<CreateRoomUiState> = _uiState

    private val _groupCreationState = MutableStateFlow<GroupCreationState>(GroupCreationState.Idle)
    val groupCreationState: StateFlow<GroupCreationState> = _groupCreationState

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

    fun createGroup(name: String, memberIds: Set<String>) {
        _groupCreationState.value = GroupCreationState.Loading
        scope.launch {
            try {
                val userId = settingsStorage.getString("user_id") ?: run {
                    _groupCreationState.value = GroupCreationState.Error("User ID not found")
                    return@launch
                }

                val finalMemberIds = if (memberIds.isEmpty()) {
                    listOf(userId)  // Just the creator
                } else {
                    (memberIds + userId).toList()
                }

                val group = groupRepository.createGroup(
                    name = name,
                    photo = null,
                    creatorId = userId,
                    memberIds = finalMemberIds
                )

                _groupCreationState.value = GroupCreationState.Success(group)
            } catch (e: Exception) {
                println("Network error: ${e.message}")
                _groupCreationState.value =
                    GroupCreationState.Error(e.message ?: "Failed to create group")
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}
