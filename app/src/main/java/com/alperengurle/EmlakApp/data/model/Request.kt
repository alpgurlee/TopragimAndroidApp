package com.alperengurle.EmlakApp.data.model

import com.google.firebase.Timestamp

data class Request(
    var id: String? = null,
    val userID: String = "",
    val city: String? = null,
    val district: String? = null,
    val neighborhood: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val timestamp: Timestamp = Timestamp.now()
)