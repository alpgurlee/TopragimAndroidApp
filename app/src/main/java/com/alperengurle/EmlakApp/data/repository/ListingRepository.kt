package com.alperengurle.EmlakApp.data.repository

import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.model.ListingDetails
import com.alperengurle.EmlakApp.util.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ListingRepository {
    private val db = FirebaseFirestore.getInstance()
    private val listingsCollection = db.collection("listings")

    // Temel İlan İşlemleri
    // ListingRepository.kt
    suspend fun getListing(listingId: String): Listing? {
        return try {
            val document = listingsCollection.document(listingId).get().await()
            val data = document.data ?: return null

            Listing(
                id = document.id,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                ownerID = data["ownerID"] as? String ?: "",
                ownerName = data["ownerName"] as? String ?: "", // default empty string ekledik
                ownerFCMToken = data["ownerFCMToken"] as? String,
                category = data["category"] as? String ?: "",
                subCategory = data["subCategory"] as? String ?: "Satılık",
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
        } catch (e: Exception) {
            null
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
                houseTrade = data["houseTrade"] as? Boolean,
                parcelQueryLink = data["parcelQueryLink"] as? String  // Bu satırı ekleyin

            )
        } else {
            ListingDetails()
        }
    }
    suspend fun deleteListing(listingId: String) {
        FirebaseFirestore.getInstance()
            .collection("listings")
            .document(listingId)
            .delete()
            .await()
    }
    fun getApprovedListings(
        onSuccess: (List<Listing>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        listingsCollection
            .whereEqualTo("status", Constants.STATUS_APPROVED)  // Değişiklik burada
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val listings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Listing::class.java)?.apply {
                        this.id = doc.id
                    }
                } ?: emptyList()

                onSuccess(listings)
            }
    }

    // Favori İşlemleri
    fun checkIfFavorite(userId: String, listingId: String, onResult: (Boolean) -> Unit) {
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(listingId)
            .get()
            .addOnSuccessListener { document ->
                onResult(document.exists())
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    suspend fun addToFavorites(userId: String, listing: Listing) {
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(listing.id ?: throw IllegalArgumentException("Listing ID cannot be null"))
            .set(mapOf(
                "timestamp" to Timestamp.now(),
                "listingId" to listing.id
            ))
            .await()

        // İlanın favori sayacını güncelle
        db.collection("listings")
            .document(listing.id!!)
            .collection("favorites")
            .document(userId)
            .set(mapOf("timestamp" to Timestamp.now()))
            .await()
    }

    suspend fun removeFromFavorites(userId: String, listingId: String) {
        db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(listingId)
            .delete()
            .await()

        // İlanın favori sayacını güncelle
        db.collection("listings")
            .document(listingId)
            .collection("favorites")
            .document(userId)
            .delete()
            .await()
    }

    fun getFavoriteListings(userId: String): Flow<List<Listing>> = callbackFlow {
        val listenerRegistration = db.collection("users")
            .document(userId)
            .collection("favorites")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val listingIds = snapshot?.documents?.mapNotNull { it.getString("listingId") }
                if (listingIds.isNullOrEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Favori ilanları getir
                db.collection("listings")
                    .whereIn(FieldPath.documentId(), listingIds)
                    .whereEqualTo("status", "approved")
                    .get()
                    .addOnSuccessListener { listingsSnapshot ->
                        val listings = listingsSnapshot.documents.mapNotNull { doc ->
                            doc.toObject(Listing::class.java)?.apply { id = doc.id }
                        }
                        trySend(listings)
                    }
                    .addOnFailureListener {
                        trySend(emptyList())
                    }
            }

        awaitClose { listenerRegistration.remove() }
    }

    // Favori Sayısını Takip Et
    fun getFavoriteCount(listingId: String): Flow<Int> = callbackFlow {
        val listenerRegistration = db.collection("listings")
            .document(listingId)
            .collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.size ?: 0)
            }

        awaitClose { listenerRegistration.remove() }
    }

    // Favori Durumunu Takip Et
    fun observeFavoriteStatus(userId: String, listingId: String): Flow<Boolean> = callbackFlow {
        val listenerRegistration = db.collection("users")
            .document(userId)
            .collection("favorites")
            .document(listingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snapshot?.exists() ?: false)
            }

        awaitClose { listenerRegistration.remove() }
    }
}