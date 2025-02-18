package com.alperengurle.EmlakApp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Listing(
    @DocumentId
    var id: String? = null,
    var title: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var ownerID: String = "",
    var ownerName: String = "", // nullable'dan non-null'a çevirdik
    var ownerFCMToken: String? = null,
    var category: String = "",
    var subCategory: String = "Satılık",
    var city: String = "",
    var district: String = "",
    var neighborhood: String? = null,
    var listingNumber: Int = 0,
    var mediaUrls: List<String> = listOf(),
    var timestamp: Timestamp = Timestamp.now(),
    var status: String = "pending",
    var details: ListingDetails = ListingDetails(),
    var isFavorited: Boolean = false,
    var titleLowercase: String = ""
)
{
    @get:Exclude
    val formattedPrice: String
        get() = String.format("%,.0f ₺", price)
}