package com.alperengurle.EmlakApp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

// app/src/main/java/com/alperengurle/EmlakApp/data/repository/UserLimitsRepository.kt
class UserLimitsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun checkAndUpdateListing(): Result<Boolean> {
        if (isAdmin()) return Result.success(true)

        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı girişi yapılmamış"))
        val limitsDoc = db.collection("userLimits").document(userId).get().await()
        val currentListings = limitsDoc.getLong("totalListings")?.toInt() ?: 0

        return if (currentListings >= 15) {
            Result.failure(Exception("En fazla 15 ilan verebilirsiniz."))
        } else {
            db.collection("userLimits").document(userId)
                .set(hashMapOf("totalListings" to (currentListings + 1)), SetOptions.merge())
                .await()
            Result.success(true)
        }
    }

    suspend fun checkAndUpdateRequest(): Result<Boolean> {
        if (isAdmin()) return Result.success(true)

        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı girişi yapılmamış"))
        val limitsDoc = db.collection("userLimits").document(userId).get().await()
        val lastDate = limitsDoc.getTimestamp("lastRequestDate")
        val dailyRequests = limitsDoc.getLong("dailyRequests")?.toInt() ?: 0

        if (lastDate?.toDate()?.isSameDay() == true) {
            if (dailyRequests >= 20) {
                return Result.failure(Exception("Günlük talep limitine ulaştınız. Yarın tekrar deneyiniz."))
            }
            db.collection("userLimits").document(userId)
                .set(hashMapOf("dailyRequests" to (dailyRequests + 1)), SetOptions.merge())
                .await()
        } else {
            db.collection("userLimits").document(userId)
                .set(hashMapOf(
                    "dailyRequests" to 1,
                    "lastRequestDate" to Timestamp.now()
                ), SetOptions.merge())
                .await()
        }
        return Result.success(true)
    }

    suspend fun checkAndUpdateOffer(): Result<Boolean> {
        if (isAdmin()) return Result.success(true)

        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı girişi yapılmamış"))
        val limitsDoc = db.collection("userLimits").document(userId).get().await()
        val lastDate = limitsDoc.getTimestamp("lastOfferDate")
        val dailyOffers = limitsDoc.getLong("dailyOffers")?.toInt() ?: 0

        if (lastDate?.toDate()?.isSameDay() == true) {
            if (dailyOffers >= 20) {
                return Result.failure(Exception("Günlük teklif limitine ulaştınız. Yarın tekrar deneyiniz."))
            }
            db.collection("userLimits").document(userId)
                .set(hashMapOf("dailyOffers" to (dailyOffers + 1)), SetOptions.merge())
                .await()
        } else {
            db.collection("userLimits").document(userId)
                .set(hashMapOf(
                    "dailyOffers" to 1,
                    "lastOfferDate" to Timestamp.now()
                ), SetOptions.merge())
                .await()
        }
        return Result.success(true)
    }

    private suspend fun isAdmin(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val userDoc = db.collection("users").document(userId).get().await()
        return userDoc.getString("role") == "admin"
    }

    // Date extension function'ı düzeltildi
    private fun Date.isSameDay(): Boolean {
        val calendar1 = Calendar.getInstance().apply { time = this@isSameDay }
        val calendar2 = Calendar.getInstance()
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }
}