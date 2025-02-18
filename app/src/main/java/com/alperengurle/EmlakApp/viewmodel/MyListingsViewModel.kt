package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.repository.ListingRepository // Ekleyin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MyListingsViewModel : ViewModel() {
    private val _listings = MutableLiveData<List<Listing>>()
    val listings: LiveData<List<Listing>> = _listings

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>() // Ekleyin
    val error: LiveData<String?> = _error // Ekleyin

    fun loadMyListings() {
        viewModelScope.launch {
            _loading.value = true
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            FirebaseFirestore.getInstance().collection("listings")
                .whereEqualTo("ownerID", userId)
                .get()
                .addOnSuccessListener { documents ->
                    _listings.value = documents.mapNotNull { doc ->
                        doc.toObject(Listing::class.java).apply { id = doc.id }
                    }
                    _loading.value = false
                }
                .addOnFailureListener {
                    _loading.value = false
                }
        }
    }

    fun toggleFavorite(listing: Listing) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val repository = ListingRepository()

                if (listing.isFavorited) {
                    listing.id?.let { listingId ->
                        repository.removeFromFavorites(userId, listingId)
                    }
                } else {
                    repository.addToFavorites(userId, listing)
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}