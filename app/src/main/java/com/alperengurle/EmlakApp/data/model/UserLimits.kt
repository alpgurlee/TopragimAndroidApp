package com.alperengurle.EmlakApp.data.model

// app/src/main/java/com/alperengurle/EmlakApp/data/model/UserLimits.kt
data class UserLimits(
    val totalListings: Int = 0,
    val dailyRequests: Int = 0,
    val dailyOffers: Int = 0,
    val lastRequestDate: com.google.firebase.Timestamp? = null,
    val lastOfferDate: com.google.firebase.Timestamp? = null
)