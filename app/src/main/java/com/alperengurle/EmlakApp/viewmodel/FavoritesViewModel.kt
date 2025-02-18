package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.repository.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {
    private val repository = ListingRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _favoriteListings = MutableLiveData<List<Listing>>()
    val favoriteListings: LiveData<List<Listing>> = _favoriteListings

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true
            repository.getFavoriteListings(userId)
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { listings ->
                    _favoriteListings.value = listings
                    _isLoading.value = false
                }
        }
    }

    fun removeFavorite(listingId: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                repository.removeFromFavorites(userId, listingId)
                // Otomatik olarak Flow güncelleyecek listeyi
            } catch (e: Exception) {
                _error.value = "Favorilerden kaldırılırken bir hata oluştu: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}