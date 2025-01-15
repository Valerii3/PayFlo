package dev.valerii.payflo.viewmodel

import dev.valerii.payflo.model.Group
import dev.valerii.payflo.model.User
import dev.valerii.payflo.repository.GroupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddExpenseViewModel(
    private val groupRepository: GroupRepository,
    private val group: Group
) {
    private val _uiState = MutableStateFlow<AddExpenseUiState>(AddExpenseUiState.Input)
    val uiState: StateFlow<AddExpenseUiState> = _uiState

    fun addExpense(name: String, amount: Double, participantIds: List<String>) {
        scope.launch {
            try {
                _uiState.value = AddExpenseUiState.Loading
                val share = amount / participantIds.size
                // Add your expense creation logic here
                _uiState.value = AddExpenseUiState.Success
            } catch (e: Exception) {
                _uiState.value = AddExpenseUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

sealed class AddExpenseUiState {
    object Input : AddExpenseUiState()
    object Loading : AddExpenseUiState()
    object Success : AddExpenseUiState()
    data class Error(val message: String) : AddExpenseUiState()
}