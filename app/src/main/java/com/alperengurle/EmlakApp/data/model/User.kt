package com.alperengurle.EmlakApp.data.model

data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profession: String? = null,
    val fcmToken: String? = null,
    val role: String = "user"
)