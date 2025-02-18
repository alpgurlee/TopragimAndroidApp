package com.alperengurle.EmlakApp.viewmodel

// SettingsViewModel.kt - app/src/main/java/com/alperengurle/EmlakApp/viewmodel/SettingsViewModel.kt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun deleteAccount() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("Kullanıcı bulunamadı")

                // Kullanıcının ilanlarını sil
                val listings = db.collection("listings")
                    .whereEqualTo("ownerID", userId)
                    .get()
                    .await()

                listings.documents.forEach { doc ->
                    doc.reference.delete().await()
                }

                // Kullanıcının tekliflerini sil
                val offers = db.collection("offers")
                    .whereEqualTo("userID", userId)
                    .get()
                    .await()

                offers.documents.forEach { doc ->
                    doc.reference.delete().await()
                }

                // Firestore'dan kullanıcı dokümanını sil
                db.collection("users").document(userId).delete().await()

                // Firebase Auth'dan kullanıcıyı sil
                auth.currentUser?.delete()?.await()

                _loading.value = false

            } catch (e: Exception) {
                _error.value = "Hesap silme işlemi başarısız oldu: ${e.message}"
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}