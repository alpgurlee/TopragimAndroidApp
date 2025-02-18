package com.alperengurle.EmlakApp.data.repository

import com.alperengurle.EmlakApp.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProfileRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getUserProfile(userId: String): User {
        val document = db.collection("users").document(userId).get().await()
        return document.toObject(User::class.java)
            ?: throw Exception("Kullanıcı bilgileri bulunamadı")
    }

    suspend fun updateProfile(
        userId: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        profession: String?
    ) {
        val updates = hashMapOf<String, Any?>(
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "profession" to profession
        )

        db.collection("users").document(userId)
            .update(updates)
            .await()
    }


    suspend fun deleteUserAccount(userId: String) {
        // Kullanıcının ilanlarını sil
        val listings = db.collection("listings")
            .whereEqualTo("ownerID", userId)
            .get()
            .await()

        for (listing in listings.documents) {
            listing.reference.delete().await()
        }

        // Kullanıcının tekliflerini sil
        val offers = db.collection("offers")
            .whereEqualTo("userID", userId)
            .get()
            .await()

        for (offer in offers.documents) {
            offer.reference.delete().await()
        }

        // Kullanıcı dökümanını sil
        db.collection("users").document(userId).delete().await()
    }
}