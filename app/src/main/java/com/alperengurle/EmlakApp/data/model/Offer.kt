package com.alperengurle.EmlakApp.data.model
import com.google.firebase.Timestamp

data class Offer(
    val id: String? = null,
    val listingID: String = "",
    val offerPrice: Double = 0.0,
    val userID: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val timestamp: Timestamp = Timestamp.now()
)