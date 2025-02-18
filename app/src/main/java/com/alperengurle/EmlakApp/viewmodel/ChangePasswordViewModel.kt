package com.alperengurle.EmlakApp.viewmodel


// ChangePasswordViewModel.kt - Oluşturulacak dosya yolu: app/src/main/java/com/alperengurle/EmlakApp/viewmodel/ChangePasswordViewModel.kt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangePasswordViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                auth.currentUser?.updatePassword(newPassword)?.await()
                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Şifre değiştirme işlemi başarısız oldu"
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}