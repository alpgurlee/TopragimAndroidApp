package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.model.User
import com.alperengurle.EmlakApp.data.repository.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class ListingDetailViewModel : ViewModel() {
    private val repository = ListingRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentListing = MutableLiveData<Listing>()
    val currentListing: LiveData<Listing> = _currentListing

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isFavorite = MutableLiveData<Boolean>()
    val isFavorite: LiveData<Boolean> = _isFavorite

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _ownerDetails = MutableLiveData<User?>()
    val ownerDetails: LiveData<User?> = _ownerDetails

    private var isAdminCache: Boolean? = null

    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getListing(listingId)?.let { listing ->
                    _currentListing.value = listing
                    checkIfFavorite(listingId)
                } ?: run {
                    _error.value = "İlan bulunamadı"
                }
            } catch (e: Exception) {
                _error.value = "İlan yüklenirken bir hata oluştu"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkIfFavorite(listingId: String) {
        auth.currentUser?.uid?.let { userId ->
            repository.checkIfFavorite(userId, listingId) { isFavorite ->
                _isFavorite.value = isFavorite
            }
        }
    }

    suspend fun isAdmin(): Boolean {
        isAdminCache?.let { return it }

        val currentUser = auth.currentUser?.uid ?: return false
        return try {
            val userDoc = db.collection("users")
                .document(currentUser)
                .get()
                .await()
            val isAdmin = userDoc.getString("role") == "admin"
            isAdminCache = isAdmin
            isAdmin
        } catch(e: Exception) {
            false
        }
    }

    suspend fun canModifyListing(): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        val listing = _currentListing.value ?: return false
        return isAdmin() || listing.ownerID == currentUserId
    }

    suspend fun deleteListing(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val listingId = _currentListing.value?.id ?: return@launch
                db.collection("listings")
                    .document(listingId)
                    .delete()
                    .await()
                onSuccess()
            } catch(e: Exception) {
                _error.value = "İlan silinirken hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun toggleFavorite() {
        val userId = auth.currentUser?.uid ?: return
        val listing = currentListing.value ?: return

        viewModelScope.launch {
            try {
                val newFavoriteState = !(_isFavorite.value ?: false)
                if (newFavoriteState) {
                    repository.addToFavorites(userId, listing)
                } else {
                    repository.removeFromFavorites(userId, listing.id ?: return@launch)
                }
                _isFavorite.value = newFavoriteState
            } catch (e: Exception) {
                _error.value = if (_isFavorite.value == true) {
                    "Favorilerden kaldırılırken bir hata oluştu"
                } else {
                    "Favorilere eklenirken bir hata oluştu"
                }
            }
        }
    }

    fun refreshListing() {
        currentListing.value?.id?.let { listingId ->
            loadListing(listingId)
        }
    }

    fun checkOwnership(): Boolean {
        return auth.currentUser?.uid == currentListing.value?.ownerID
    }

    fun getCurrentListingId(): String? {
        return currentListing.value?.id
    }
}