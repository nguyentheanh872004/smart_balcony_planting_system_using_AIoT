package com.example.hihi.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hihi.network.ApiClient
import com.example.hihi.network.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<Boolean?>(null)
    val loginState: StateFlow<Boolean?> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.api.login(
                    LoginRequest(username, password)
                )

                _loginState.value = response.success
            } catch (e: Exception) {
                _loginState.value = false
            }
        }
    }

    fun resetState() {
        _loginState.value = null
    }
}
