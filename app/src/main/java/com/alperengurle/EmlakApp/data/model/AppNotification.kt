package com.alperengurle.EmlakApp.data.model

import com.alperengurle.EmlakApp.R

// app/src/main/java/com/alperengurle/EmlakApp/data/model/AppNotification.kt
data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.NEWOFFER,  // NEW_OFFER yerine NEWOFFER kullanıyoruz
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val relatedId: String? = null,
    var isRead: Boolean = false
)
// app/src/main/java/com/alperengurle/EmlakApp/data/model/NotificationType.kt
enum class NotificationType {
    NEWOFFER,           // newOffer
    OFFERAPPROVED,      // offerApproved
    OFFERREJECTED,      // offerRejected
    LISTINGAPPROVED,    // listingApproved
    LISTINGREJECTED,    // listingRejected
    NEWREQUEST,         // newRequest
    MATCHINGLISTING,    // matchingListing
    NEWLISTING,         // newListing
    OFFERUPDATE;        // offerUpdate

    companion object {
        fun fromString(value: String): NotificationType {
            return try {
                when (value) {
                    "newOffer" -> NEWOFFER
                    "offerApproved" -> OFFERAPPROVED
                    "offerRejected" -> OFFERREJECTED
                    "listingApproved" -> LISTINGAPPROVED
                    "listingRejected" -> LISTINGREJECTED
                    "newRequest" -> NEWREQUEST
                    "matchingListing" -> MATCHINGLISTING
                    "newListing" -> NEWLISTING
                    "offerUpdate" -> OFFERUPDATE
                    else -> NEWOFFER
                }
            } catch (e: Exception) {
                NEWOFFER
            }
        }
    }

    fun getIcon(): Int {
        return when (this) {
            NEWOFFER, OFFERAPPROVED, OFFERREJECTED, OFFERUPDATE -> R.drawable.ic_tag
            LISTINGAPPROVED, LISTINGREJECTED, NEWLISTING -> R.drawable.ic_home
            NEWREQUEST, MATCHINGLISTING -> R.drawable.ic_search
        }
    }

    fun getColor(): Int {
        return when (this) {
            NEWOFFER, NEWREQUEST, NEWLISTING -> R.color.blue
            LISTINGAPPROVED, OFFERAPPROVED -> R.color.green
            LISTINGREJECTED, OFFERREJECTED -> R.color.red
            MATCHINGLISTING, OFFERUPDATE -> R.color.purple
        }
    }
}