package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AccountDetailsViewModel : ViewModel() {
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun updateUserDetails(
        userId: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        profession: String?,
        birthDate: String?
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val updates = hashMapOf<String, Any>().apply {
                    put("firstName", firstName)
                    put("lastName", lastName)
                    put("phoneNumber", phoneNumber)
                    profession?.let { put("profession", it) }
                    birthDate?.let { put("birthDate", it) }
                }

                db.collection("users")
                    .document(userId)
                    .update(updates)
                    .await()

                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}