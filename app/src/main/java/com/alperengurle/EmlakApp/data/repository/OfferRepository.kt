package com.alperengurle.EmlakApp.data.repository

import com.alperengurle.EmlakApp.data.model.Offer
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OfferRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getUserOffers(userId: String): Flow<List<Offer>> = callbackFlow {
        val listenerRegistration = db.collection("offers")
            .whereEqualTo("userID", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20) // Son 20 teklif
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val offers = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Offer::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(offers)
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun submitOffer(listingId: String, amount: Double): Result<String> {
        return try {
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: throw Exception("Kullanıcı girişi yapılmamış")

            val userDoc = db.collection("users").document(userId).get().await()
            val userData = userDoc.data ?: throw Exception("Kullanıcı bilgileri bulunamadı")

            val offer = hashMapOf(
                "listingID" to listingId,
                "offerPrice" to amount,
                "userID" to userId,
                "userName" to "${userData["firstName"]} ${userData["lastName"]}",
                "userEmail" to auth.currentUser?.email,
                "userPhone" to (userData["phoneNumber"] ?: ""),
                "timestamp" to Timestamp.now()
            )

            val result = db.collection("offers").add(offer).await()
            Result.success(result.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOffer(offerId: String) {
        db.collection("offers").document(offerId).delete().await()
    }

    fun getListingOffers(listingId: String): Flow<List<Offer>> = callbackFlow {
        val listenerRegistration = db.collection("offers")
            .whereEqualTo("listingID", listingId)
            .orderBy("offerPrice", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val offers = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Offer::class.java)?.copy(id = doc.id)
                } ?: emptyList()


                trySend(offers)
            }

        awaitClose { listenerRegistration.remove() }
    }
}