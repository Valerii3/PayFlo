package dev.valerii.payflo.viewmodel

import dev.valerii.payflo.model.Group
import dev.valerii.payflo.repository.GroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// First, create GroupDetailViewModel:
class GroupDetailViewModel(
    private val groupRepository: GroupRepository,
    private val groupId: String
) {
    private val _uiState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val uiState: StateFlow<GroupDetailUiState> = _uiState

    fun loadGroup() {
        _uiState.value = GroupDetailUiState.Loading
        scope.launch {
            try {
                groupRepository.getGroup(groupId)?.let { group ->
                    _uiState.value = GroupDetailUiState.Success(group)
                } ?: run {
                    _uiState.value = GroupDetailUiState.Error("Group not found")
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
    data class Success(val group: Group) : GroupDetailUiState()
    data class Error(val message: String) : GroupDetailUiState()
}