package com.alperengurle.EmlakApp.ui.listing

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.alperengurle.EmlakApp.data.repository.UserLimitsRepository // Import eklendi


class AddListingViewModel : ViewModel() {
    private val userLimitsRepository = UserLimitsRepository()
    private val _listingSaved = MutableLiveData<Boolean>()
    val listingSaved: LiveData<Boolean> = _listingSaved

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun saveListing(
        listingData: HashMap<String, Any>,
        images: List<Uri>,
        videos: List<Uri>
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("AddListingViewModel", "Başlangıç - İlan kaydetme işlemi başladı")

                // Kullanıcı bilgilerini al
                val userId = auth.currentUser?.uid ?: throw Exception("Kullanıcı girişi yapılmamış")
                val userDoc = db.collection("users").document(userId).get().await()

                // iOS ile uyumlu olması için bilgileri ekleyelim
                listingData["ownerID"] = userId
                // iOS formatına uygun olarak email kullanıyoruz
                listingData["ownerName"] = auth.currentUser?.email ?: ""
                listingData["subCategory"] = "Satılık"

                // Medyaları yükle
                val mediaUrls = mutableListOf<String>()

                // Resimleri yükle
                Log.d("AddListingViewModel", "Resim yükleme başladı: ${images.size} resim")
                images.forEach { uri ->
                    val imageUrl = uploadMedia(uri, "images")
                    mediaUrls.add(imageUrl)
                }
                val limitCheck = userLimitsRepository.checkAndUpdateListing()
                if (limitCheck.isFailure) {
                    limitCheck.exceptionOrNull()?.message?.let {
                        _error.postValue(it)
                        return@launch
                    }
                }
                // Videoları yükle
                Log.d("AddListingViewModel", "Video yükleme başladı: ${videos.size} video")
                videos.forEach { uri ->
                    val videoUrl = uploadMedia(uri, "videos")
                    mediaUrls.add(videoUrl)
                }

                // Listing verisini hazırla
                listingData.apply {
                    put("mediaUrls", mediaUrls)
                    put("timestamp", Timestamp.now())
                    put("titleLowercase", (get("title") as String).lowercase())
                    put("status", "pending")  // iOS'ta kullanılan format
                    put("listingNumber", getNextListingNumber())
                    put("ownerFCMToken", userDoc.getString("fcmToken") ?: "")

                    // iOS'ta olmayan alanları kaldır
                    remove("createdAt")
                    remove("updatedAt")
                    remove("ownerEmail")
                }

                Log.d("AddListingViewModel", "Firestore'a kaydetme başlıyor: $listingData")

                // Firestore'a kaydet
                val docRef = db.collection("listings")
                    .add(listingData)
                    .await()

                Log.d("AddListingViewModel", "İlan başarıyla kaydedildi: ${docRef.id}")
                _listingSaved.postValue(true)

            } catch (e: Exception) {
                Log.e("AddListingViewModel", "Hata oluştu", e)
                _error.postValue(e.message ?: "Bir hata oluştu")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun uploadMedia(uri: Uri, folder: String): String {
        return withContext(Dispatchers.IO) {
            val storageRef = storage.reference
                .child("listing_$folder")
                .child("${System.currentTimeMillis()}_${auth.currentUser?.uid}")

            try {
                val uploadTask = storageRef.putFile(uri).await()
                return@withContext uploadTask.storage.downloadUrl.await().toString()
            } catch (e: Exception) {
                throw Exception("Medya yüklenirken hata oluştu: ${e.message}")
            }
        }
    }

    private suspend fun getNextListingNumber(): Int {
        return try {
            val snapshot = db.collection("listings")
                .orderBy("listingNumber", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                1
            } else {
                val lastNumber = snapshot.documents[0].getLong("listingNumber") ?: 0
                lastNumber.toInt() + 1
            }
        } catch (e: Exception) {
            throw Exception("İlan numarası alınırken hata oluştu: ${e.message}")
        }
    }
}