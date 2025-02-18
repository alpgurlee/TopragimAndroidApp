package com.alperengurle.EmlakApp.ui.offers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.alperengurle.EmlakApp.data.repository.UserLimitsRepository // Import eklendi



class MakeOfferViewModel : ViewModel() {
    private val userLimitsRepository = UserLimitsRepository()

    private val _offerResult = MutableLiveData<Result<Unit>>()
    val offerResult: LiveData<Result<Unit>> = _offerResult

    private val _listingPrice = MutableLiveData<Double>()
    val listingPrice: LiveData<Double> = _listingPrice

    fun submitOffer(listingId: String, amount: Double) {
        viewModelScope.launch {
            try {
                // İlan fiyatını kontrol et
                val listing = FirebaseFirestore.getInstance()
                    .collection("listings")
                    .document(listingId)
                    .get()
                    .await()

                val listingPrice = listing.getDouble("price") ?: 0.0
                val minimumOffer = listingPrice * 0.60

                if (amount < minimumOffer) {
                    _offerResult.value = Result.Error("Verdiğiniz teklif bu ilan için çok düşüktür. ")
                    return@launch
                }
                val limitCheck = userLimitsRepository.checkAndUpdateOffer()
                if (limitCheck.isFailure) {
                    limitCheck.exceptionOrNull()?.message?.let {
                        _offerResult.value = Result.Error(it)
                        return@launch
                    }
                }
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid ?: throw Exception("Kullanıcı girişi yapılmamış")

                // Kullanıcı bilgilerini Firestore'dan al
                val db = FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(userId).get().await()

                val offer = hashMapOf(
                    "listingID" to listingId,
                    "offerPrice" to amount,
                    "userID" to userId,
                    "userName" to "${userDoc.getString("firstName")} ${userDoc.getString("lastName")}",
                    "userEmail" to userDoc.getString("email"),
                    "userPhone" to (userDoc.getString("phoneNumber") ?: ""),
                    "timestamp" to Timestamp.now()
                )

                db.collection("offers")
                    .add(offer)
                    .await()

                _offerResult.value = Result.Success(Unit)

            } catch (e: Exception) {
                _offerResult.value = Result.Error(e.message ?: "Bir hata oluştu")
            }
        }
    }
}