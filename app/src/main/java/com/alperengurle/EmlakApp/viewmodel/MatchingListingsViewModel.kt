package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.model.Request
import com.alperengurle.EmlakApp.data.repository.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MatchingListingsViewModel : ViewModel() {
    private val _matchingListings = MutableLiveData<List<Listing>>()
    val matchingListings: LiveData<List<Listing>> = _matchingListings

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val db = FirebaseFirestore.getInstance()
    private val allMatchingListings = mutableListOf<Listing>()

    fun loadMatchingListings(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = db.collection("requests")
                    .document(requestId)
                    .get()
                    .await()
                    .toObject(Request::class.java)

                request?.let { req ->
                    var query = db.collection("listings")
                        .whereEqualTo("status", "approved")

                    if (!req.city.isNullOrEmpty() && req.city != "Tüm Şehirler") {
                        query = query.whereEqualTo("city", req.city)
                    }

                    val listings = query.get().await().documents.mapNotNull { doc ->
                        doc.toObject(Listing::class.java)?.apply { id = doc.id }
                    }.filter { listing ->
                        val matchesMinPrice = req.minPrice?.let { listing.price >= it } ?: true
                        val matchesMaxPrice = req.maxPrice?.let { listing.price <= it } ?: true
                        matchesMinPrice && matchesMaxPrice
                    }

                    // Yeni eşleşen ilanları listeye ekle
                    allMatchingListings.addAll(listings)

                    // Tekrar eden ilanları filtrele
                    val uniqueListings = allMatchingListings.distinctBy { it.id }

                    _matchingListings.value = uniqueListings
                }
            } catch (e: Exception) {
                _error.value = "Eşleşen ilanlar yüklenirken bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMatchingListings() {
        allMatchingListings.clear()
        _matchingListings.value = emptyList()
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
                _error.postValue("Favori işlemi başarısız: ${e.message}")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}