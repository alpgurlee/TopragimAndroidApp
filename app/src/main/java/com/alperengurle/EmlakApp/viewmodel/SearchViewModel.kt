package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.repository.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SearchViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val listingRepository = ListingRepository()

    private val _searchResults = MutableLiveData<List<Listing>>()
    val searchResults: LiveData<List<Listing>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentQuery = ""
    private var selectedCity: String? = null
    private var selectedDistrict: String? = null
    private var selectedCategory: String? = null
    private var minPrice: Long = 0
    private var maxPrice: Long = 10_000_000

    init {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // İlk açılışta tüm onaylı ilanları getir
                val initialQuery = db.collection("listings")
                    .whereEqualTo("status", "approved")

                val querySnapshot = initialQuery.get().await()
                val listings = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Listing::class.java)?.apply { id = doc.id }
                }
                _searchResults.value = listings
            } catch (e: Exception) {
                _error.value = e.message
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                currentQuery = query.trim()

                if (currentQuery.matches(Regex("\\d+"))) {
                    // İlan numarası ile arama
                    searchByListingNumber(currentQuery.toInt())
                } else {
                    // Kelime ile arama
                    var query = db.collection("listings")
                        .whereEqualTo("status", "approved")

                    // Filtreleri uygula
                    query = applyFilters(query)

                    val querySnapshot = query.get().await()
                    val allListings = querySnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Listing::class.java)?.apply { id = doc.id }
                    }

                    // Başlık ve açıklamada arama yap
                    if (currentQuery.isNotEmpty()) {
                        _searchResults.value = allListings.filter { listing ->
                            listing.title.contains(currentQuery, ignoreCase = true) ||
                                    listing.description.contains(currentQuery, ignoreCase = true)
                        }
                    } else {
                        _searchResults.value = allListings
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun searchByListingNumber(listingNumber: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val querySnapshot = db.collection("listings")
                    .whereEqualTo("listingNumber", listingNumber)
                    .whereEqualTo("status", "approved")
                    .get()
                    .await()

                _searchResults.value = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Listing::class.java)?.apply { id = doc.id }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun executeSearch() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                var query = db.collection("listings")
                    .whereEqualTo("status", "approved")

                query = applyFilters(query)

                val querySnapshot = query.get().await()
                _searchResults.value = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Listing::class.java)?.apply { id = doc.id }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyFilters(query: Query): Query {
        var filteredQuery = query

        // Kategori ve konum filtrelerini uygula
        selectedCategory?.let { category ->
            filteredQuery = filteredQuery.whereEqualTo("category", category)
        }

        selectedCity?.let { city ->
            filteredQuery = filteredQuery.whereEqualTo("city", city)
        }

        selectedDistrict?.let { district ->
            filteredQuery = filteredQuery.whereEqualTo("district", district)
        }

        // Fiyat filtresi her zaman uygulanmalı
        filteredQuery = filteredQuery
            .whereGreaterThanOrEqualTo("price", minPrice)
            .whereLessThanOrEqualTo("price", maxPrice)
            .orderBy("price") // Fiyata göre sıralama ekleyelim

        return filteredQuery
    }


    fun updateLocation(city: String?, district: String?) {
        selectedCity = city
        selectedDistrict = district
        executeSearch()
    }

    fun updateCategory(category: String?) {
        selectedCategory = category
        executeSearch()
    }

    fun updatePriceRange(min: Long, max: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                minPrice = min
                maxPrice = max

                // Fiyat güncellendiğinde yeni aramayı yap
                var query = db.collection("listings")
                    .whereEqualTo("status", "approved")

                query = applyFilters(query)

                val querySnapshot = query.get().await()
                _searchResults.value = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Listing::class.java)?.apply { id = doc.id }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(listing: Listing) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                if (listing.isFavorited) {
                    listing.id?.let { listingId ->
                        listingRepository.removeFromFavorites(userId, listingId)
                    }
                } else {
                    listingRepository.addToFavorites(userId, listing)
                }
                refreshCurrentSearch()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun refreshCurrentSearch() {
        if (currentQuery.isNotEmpty()) {
            search(currentQuery)
        } else {
            executeSearch()
        }
    }

    fun clearAllFilters() {
        currentQuery = ""
        selectedCity = null
        selectedDistrict = null
        selectedCategory = null
        minPrice = 0
        maxPrice = 10_000_000
        executeSearch()
    }

    fun clearError() {
        _error.value = null
    }
}