package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Request
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class RequestsViewModel : ViewModel() {
    private val _requests = MutableLiveData<List<Request>>()
    val requests: LiveData<List<Request>> = _requests

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Yeni hata kontrolü için LiveData tanımlaması
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun loadRequests() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("requests")
            .whereEqualTo("userID", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = "Veriler yüklenirken bir hata oluştu: ${error.message}"
                    return@addSnapshotListener
                }

                val requestsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Request::class.java)?.apply {
                        id = doc.id
                    }
                } ?: emptyList()

                _requests.value = requestsList
            }
    }

    fun deleteRequest(requestId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                db.collection("requests")
                    .document(requestId)
                    .delete()
                    .addOnSuccessListener {
                        _isLoading.value = false
                        // Silme işlemi başarılı olduğunda listeyi yenile
                        loadRequests()
                    }
                    .addOnFailureListener { e ->
                        _isLoading.value = false
                        _error.value = "Talep silinirken bir hata oluştu: ${e.message}"
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Talep silinirken bir hata oluştu: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
