package com.alperengurle.EmlakApp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.alperengurle.EmlakApp.ui.auth.LoginActivity
import com.alperengurle.EmlakApp.util.ThemePreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            if (ThemePreferences.isDarkMode(this))
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startLoginActivity()
            return
        }

        // Ziyaretçi kontrolü ekleyin
        setupNavigation()
        setupFirebaseMessaging()
        subscribeToAdminTopicIfNeeded()
    }

    private fun setupFirebaseMessaging() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                saveFCMToken(token)
            }
        }
    }

    private fun saveFCMToken(token: String) {
        val userId = auth.currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                println("FCM Token başarıyla kaydedildi")
            }
            .addOnFailureListener { e ->
                println("FCM Token kaydedilemedi: ${e.message}")
            }
    }

    private fun subscribeToAdminTopicIfNeeded() {
        val userId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.getString("role") == "admin") {
                    FirebaseMessaging.getInstance().subscribeToTopic("admin")
                        .addOnSuccessListener {
                            println("Admin topic'ine başarıyla abone olundu")
                        }
                        .addOnFailureListener { e ->
                            println("Admin topic'ine abone olunamadı: ${e.message}")
                        }
                }
            }
    }

    // MainActivity.kt
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setupWithNavController(navController)

        // Ziyaretçi kontrolü
        if (auth.currentUser?.isAnonymous == true) {
            bottomNavigationView.menu.apply {
                // Profil sekmesi görünür ama diğerleri gizli
                findItem(R.id.navigation_offers)?.isVisible = false
                findItem(R.id.navigation_requests)?.isVisible = false
                // Profil sekmesini aktif bırak
                findItem(R.id.navigation_profile)?.isVisible = true
            }
        }
    }

    private fun startLoginActivity() {
        Intent(this, LoginActivity::class.java).also { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        AppCompatDelegate.setDefaultNightMode(
            if (ThemePreferences.isDarkMode(this))
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}