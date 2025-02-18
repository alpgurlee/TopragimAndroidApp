package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Request
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.alperengurle.EmlakApp.data.repository.UserLimitsRepository // Import eklendi


class AddRequestViewModel : ViewModel() {
    private val userLimitsRepository = UserLimitsRepository()

    private val _requestSaved = MutableLiveData<Boolean>()
    val requestSaved: LiveData<Boolean> = _requestSaved

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun saveRequest(
        city: String?,
        district: String?,
        neighborhood: String?,
        minPrice: Double?,
        maxPrice: Double?,
        category: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid ?: throw Exception("Kullanıcı girişi yapılmamış")

                val request = Request(
                    userID = userId,
                    city = city,
                    district = district,
                    neighborhood = neighborhood,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    category = category,
                    timestamp = Timestamp.now()
                )
                val limitCheck = userLimitsRepository.checkAndUpdateRequest()
                if (limitCheck.isFailure) {
                    limitCheck.exceptionOrNull()?.message?.let {
                        _error.value = it
                        return@launch
                    }
                }
                FirebaseFirestore.getInstance()
                    .collection("requests")
                    .add(request)
                    .await()

                _requestSaved.value = true
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