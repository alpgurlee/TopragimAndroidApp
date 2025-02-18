// app/src/main/java/com/alperengurle/EmlakApp/ui/notifications/NotificationsViewModel.kt
package com.alperengurle.EmlakApp.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alperengurle.EmlakApp.data.model.AppNotification
import com.alperengurle.EmlakApp.data.model.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationsViewModel : ViewModel() {
    private val _notifications = MutableLiveData<List<AppNotification>>()
    val notifications: LiveData<List<AppNotification>> = _notifications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun fetchNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = auth.currentUser?.uid ?: throw Exception("Kullanıcı girişi yapılmamış")

                val isAdmin = checkIfAdmin(userId)
                val notificationsRef = if (isAdmin) {
                    db.collection("adminNotifications")
                } else {
                    db.collection("users").document(userId).collection("notifications")
                }

                val snapshot = notificationsRef
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val notificationsList = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    AppNotification(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        message = data["message"] as? String ?: "",
                        type = NotificationType.fromString(data["type"] as? String ?: "newOffer"),
                        timestamp = data["timestamp"] as? com.google.firebase.Timestamp
                            ?: com.google.firebase.Timestamp.now(),
                        relatedId = data["relatedID"] as? String,
                        isRead = data["isRead"] as? Boolean ?: false
                    )
                }
                _notifications.value = notificationsList
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val isAdmin = checkIfAdmin(userId)

                val notificationRef = if (isAdmin) {
                    db.collection("adminNotifications").document(notificationId)
                } else {
                    db.collection("users").document(userId)
                        .collection("notifications").document(notificationId)
                }

                notificationRef.update(
                    mapOf(
                        "isRead" to true,
                        "readAt" to com.google.firebase.Timestamp.now(),
                        "readByUserID" to userId
                    )
                ).await()

                // Yerel listeyi güncelle
                _notifications.value = _notifications.value?.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val isAdmin = checkIfAdmin(userId)

                val notificationRef = if (isAdmin) {
                    db.collection("adminNotifications").document(notificationId)
                } else {
                    db.collection("users").document(userId)
                        .collection("notifications").document(notificationId)
                }

                notificationRef.delete().await()

                // Yerel listeden kaldır
                _notifications.value = _notifications.value?.filter { it.id != notificationId }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private suspend fun checkIfAdmin(userId: String): Boolean {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            userDoc.getString("role") == "admin"
        } catch (e: Exception) {
            false
        }
    }

    fun clearError() {
        _error.value = null
    }
}