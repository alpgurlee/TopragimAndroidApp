package com.alperengurle.EmlakApp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.model.ListingDetails
import com.alperengurle.EmlakApp.util.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminPanelViewModel : ViewModel() {
    private val _listings = MutableLiveData<List<Listing>>()
    val listings: LiveData<List<Listing>> = _listings

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val db = FirebaseFirestore.getInstance()

    fun loadListings(status: String) {
        _loading.value = true
        db.collection("listings")
            .whereEqualTo("status", status)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val listingsList = snapshot.documents.mapNotNull { doc ->
                        // Doküman verilerini manuel olarak dönüştürelim
                        val data = doc.data ?: return@mapNotNull null

                        Listing(
                            id = doc.id,
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                            ownerID = data["ownerID"] as? String ?: "",
                            ownerName = data["ownerName"] as? String ?: "", // default empty string ekledik
                            ownerFCMToken = data["ownerFCMToken"] as? String,
                            category = data["category"] as? String ?: "",
                            subCategory = data["subCategory"] as? String ?: "",
                            city = data["city"] as? String ?: "",
                            district = data["district"] as? String ?: "",
                            neighborhood = data["neighborhood"] as? String,
                            listingNumber = (data["listingNumber"] as? Number)?.toInt() ?: 0,
                            mediaUrls = (data["mediaUrls"] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                            timestamp = data["timestamp"] as? Timestamp ?: Timestamp.now(),
                            status = data["status"] as? String ?: "pending",
                            details = parseListingDetails(data["details"] as? Map<String, Any>),
                            isFavorited = false
                        )
                    }
                    _listings.value = listingsList
                    _loading.value = false
                } catch (e: Exception) {
                    _error.value = e.message
                    _loading.value = false
                }
            }
            .addOnFailureListener {
                _error.value = it.message
                _loading.value = false
            }
    }
    private fun parseListingDetails(data: Map<String, Any>?): ListingDetails {
        return if (data != null) {
            ListingDetails(
                zoningStatus = data["zoningStatus"] as? String,
                areaSize = data["areaSize"] as? String,
                adaNo = data["adaNo"] as? String,
                parselNo = data["parselNo"] as? String,
                deedStatus = data["deedStatus"] as? String,
                carTrade = data["carTrade"] as? Boolean,
                houseTrade = data["houseTrade"] as? Boolean
            )
        } else {
            ListingDetails()
        }
    }
    fun loadPendingListings() {
        viewModelScope.launch {
            try {
                _loading.value = true

                FirebaseFirestore.getInstance()
                    .collection("listings")
                    .whereEqualTo("status", "pending")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .mapNotNull { doc ->
                        doc.toObject(Listing::class.java).apply { id = doc.id }
                    }
                    .let { _listings.postValue(it) }
            } catch(e: Exception) {
                _error.postValue(e.message)
            } finally {
                _loading.value = false
            }
        }
    }

    fun approveListing(listingId: String) {
        updateListingStatus(listingId, Constants.STATUS_APPROVED)
    }

    fun rejectListing(listingId: String) {
        updateListingStatus(listingId, Constants.STATUS_REJECTED)
    }

    private fun updateListingStatus(listingId: String, status: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                db.collection("listings")
                    .document(listingId)
                    .update("status", status)
                    .await()

                // Reload listings after update
                loadListings(status)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}