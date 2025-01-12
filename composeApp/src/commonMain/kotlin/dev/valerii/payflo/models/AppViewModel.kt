package dev.valerii.payflo.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel {
    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> get() = _userName

    private val _login = MutableStateFlow<String?>(null)
    val login: StateFlow<String?> get() = _login

    fun setUserName(name: String) {
        _userName.value = name
    }

    fun setLogin(login: String) {
        // Check if the login is unique (simple placeholder logic for now)
        if (isLoginUnique(login)) {
            _login.value = login
        } else {
            throw IllegalArgumentException("Login must be unique")
        }
    }

    private fun isLoginUnique(login: String): Boolean {
        // Placeholder: Replace with your logic for validating unique login
        return login.length > 3
    }
}
