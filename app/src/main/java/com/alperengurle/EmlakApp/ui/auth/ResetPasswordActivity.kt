// ResetPasswordActivity.kt
package com.alperengurle.EmlakApp.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alperengurle.EmlakApp.databinding.ActivityResetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupListeners()
    }

    private fun setupListeners() {
        binding.resetButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                binding.emailEditText.error = "Lütfen e-posta adresinizi girin"
                return@setOnClickListener
            }

            showLoading(true)
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    showLoading(false)
                    if (task.isSuccessful) {
                        showSuccessMessage()
                    } else {
                        showError(task.exception?.message ?: "Bir hata oluştu")
                    }
                }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            resetButton.isEnabled = !show
            if (show) {
                resetButton.text = "Gönderiliyor..."
            } else {
                resetButton.text = "Şifre Sıfırlama Bağlantısı Gönder"
            }
        }
    }

    private fun showSuccessMessage() {
        Toast.makeText(
            this,
            "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}