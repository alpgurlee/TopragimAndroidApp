package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.model.Offer
import com.alperengurle.EmlakApp.data.repository.ListingRepository
import com.alperengurle.EmlakApp.data.repository.OfferRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OffersViewModel : ViewModel() {
    private val offerRepository = OfferRepository()
    private val listingRepository = ListingRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var currentListingId: String? = null
    private val cache = mutableMapOf<String, Listing>()

    private val _offers = MutableLiveData<List<OfferWithListing>>()
    val offers: LiveData<List<OfferWithListing>> = _offers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadOffers()
    }

    fun loadOffers() {
        currentListingId = null
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                offerRepository.getUserOffers(userId)
                    .catch { e ->
                        _error.value = e.message
                        _isLoading.value = false
                    }
                    .collect { offers ->
                        if (currentListingId != null) return@collect

                        val offersWithListings = offers.mapNotNull { offer ->
                            val listing = cache.getOrPut(offer.listingID) {
                                listingRepository.getListing(offer.listingID) ?: return@mapNotNull null
                            }
                            OfferWithListing(offer, listing)
                        }
                        _offers.value = offersWithListings
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = "Teklifler yüklenirken hata oluştu: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun loadListingOffers(listingId: String) {
        currentListingId = listingId
        viewModelScope.launch {
            try {
                _isLoading.value = true
                offerRepository.getListingOffers(listingId)
                    .catch { e ->
                        _error.value = e.message
                        _isLoading.value = false
                    }
                    .collect { offers ->
                        if (currentListingId != listingId) return@collect

                        val offersWithListings = offers.mapNotNull { offer ->
                            val listing = cache.getOrPut(offer.listingID) {
                                listingRepository.getListing(offer.listingID) ?: return@mapNotNull null
                            }
                            OfferWithListing(offer, listing)
                        }
                        _offers.value = offersWithListings
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = "Teklifler yüklenirken hata oluştu: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun deleteOffer(offerId: String) {
        viewModelScope.launch {
            try {
                offerRepository.deleteOffer(offerId)
                _offers.value = _offers.value?.filter { it.offer.id != offerId }
            } catch (e: Exception) {
                _error.value = "Teklif silinirken hata oluştu: ${e.message}"
            }
        }
    }

    suspend fun isAdmin(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUser = auth.currentUser?.uid ?: return@withContext false
            val userDoc = db.collection("users")
                .document(currentUser)
                .get()
                .await()
            return@withContext userDoc.getString("role") == "admin"
        } catch(e: Exception) {
            return@withContext false
        }
    }

    fun clearError() {
        _error.value = null
    }

    data class OfferWithListing(
        val offer: Offer,
        val listing: Listing
    )
}