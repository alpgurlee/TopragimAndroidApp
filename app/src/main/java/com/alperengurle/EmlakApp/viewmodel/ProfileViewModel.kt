package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _userDetails = MutableLiveData<User>()
    val userDetails: LiveData<User> = _userDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadUserDetails()
    }

    fun loadUserDetails() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = auth.currentUser?.uid ?: throw Exception("Kullanıcı bulunamadı")

                val document = db.collection("users").document(userId).get().await()
                document.toObject(User::class.java)?.let { user ->
                    _userDetails.value = user
                } ?: throw Exception("Kullanıcı bilgileri alınamadı")

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun updateUserDetails(
        userId: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        profession: String?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val updates = hashMapOf<String, Any>(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "phoneNumber" to phoneNumber
                )
                profession?.let { updates["profession"] = it }

                db.collection("users").document(userId)
                    .update(updates)
                    .await()

                loadUserDetails() // Güncel bilgileri yükle
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val user = auth.currentUser ?: throw Exception("Kullanıcı bulunamadı")
                val userId = user.uid

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

                // Kullanıcının favorilerini sil
                val favorites = db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .get()
                    .await()

                favorites.documents.forEach { doc ->
                    doc.reference.delete().await()
                }

                // Kullanıcı dökümanını sil
                db.collection("users").document(userId).delete().await()

                // Firebase Auth'dan kullanıcıyı sil
                user.delete().await()

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}