package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.repository.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

enum class SortType {
    DATE_DESC, DATE_ASC, PRICE_DESC, PRICE_ASC
}

class HomeViewModel : ViewModel() {
    private val repository = ListingRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _listings = MutableLiveData<List<Listing>>()
    val listings: LiveData<List<Listing>> = _listings

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var allListings = listOf<Listing>()
    private var currentFilter: String? = null

    fun loadListings() {
        _isLoading.value = true
        repository.getApprovedListings(
            onSuccess = { listings ->
                allListings = listings
                applyFilter()
                _isLoading.value = false
            },
            onError = { exception ->
                _error.value = exception.message
                _isLoading.value = false
            }
        )
    }

    fun sortListings(sortType: SortType) {
        val sortedList = when(sortType) {
            SortType.DATE_DESC -> allListings.sortedByDescending { it.timestamp }
            SortType.DATE_ASC -> allListings.sortedBy { it.timestamp }
            SortType.PRICE_DESC -> allListings.sortedByDescending { it.price }
            SortType.PRICE_ASC -> allListings.sortedBy { it.price }
        }
        _listings.value = sortedList
    }

    fun searchListings(query: String) {
        val filteredList = if (query.matches(Regex("^\\d+$"))) {
            // İlan numarası araması
            allListings.filter { it.listingNumber.toString() == query }
        } else {
            // İlan başlığı araması
            allListings.filter { it.title.contains(query, ignoreCase = true) }
        }
        _listings.value = filteredList
    }

    fun filterListings(category: String?) {
        currentFilter = category
        applyFilter()
    }

    private fun applyFilter() {
        _listings.value = if (currentFilter == null) {
            allListings
        } else {
            allListings.filter { it.category == currentFilter }
        }
    }

    fun toggleFavorite(listing: Listing) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                if (listing.isFavorited) {
                    repository.removeFromFavorites(userId, listing.id ?: return@launch)
                } else {
                    repository.addToFavorites(userId, listing)
                }
                val updatedList = allListings.map {
                    if (it.id == listing.id) {
                        it.copy(isFavorited = !it.isFavorited)
                    } else {
                        it
                    }
                }
                allListings = updatedList
                applyFilter()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}