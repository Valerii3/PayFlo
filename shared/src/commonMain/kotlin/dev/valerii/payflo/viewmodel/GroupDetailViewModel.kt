package dev.valerii.payflo.viewmodel

import dev.valerii.payflo.model.Group
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.repository.UserRepository
import dev.valerii.payflo.storage.SettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GroupDetailViewModel(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,

    private val groupId: String
) {
    private val _uiState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val uiState: StateFlow<GroupDetailUiState> = _uiState

    fun loadGroup() {
        _uiState.value = GroupDetailUiState.Loading
        scope.launch {
            try {

                val currentUserId = userRepository.getSavedCredentials()?.userId!!
                val group = groupRepository.getGroup(groupId)
                val expenses = groupRepository.getGroupExpenses(groupId)

                if (group != null && expenses.isSuccess) {
                    _uiState.value = GroupDetailUiState.Success(
                        group = group.copy(expenses = expenses.getOrDefault(emptyList())),
                        userId = currentUserId
                    )
                } else {
                    _uiState.value = GroupDetailUiState.Error("Failed to load group")
                }
            } catch (e: Exception) {
                _uiState.value = GroupDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

sealed class GroupDetailUiState {
    object Loading : GroupDetailUiState()
    data class Success(val group: Group, val userId: String) : GroupDetailUiState()
    data class Error(val message: String) : GroupDetailUiState()
}