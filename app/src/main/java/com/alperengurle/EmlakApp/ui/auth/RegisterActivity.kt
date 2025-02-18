package com.alperengurle.EmlakApp.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.databinding.ActivityRegisterBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPhoneVerificationRequired = false
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupPhoneNumberValidation()
        setupListeners()
    }

    private fun setupPhoneNumberValidation() {
        binding.phoneEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val phone = s.toString()
                if (phone.isNotEmpty()) {
                    val digitsOnly = phone.filter { it.isDigit() }

                    if (!phone.startsWith("05")) {
                        if (phone.startsWith("5")) {
                            binding.phoneEditText.setText("0$digitsOnly")
                            binding.phoneEditText.setSelection(binding.phoneEditText.length())
                        } else {
                            binding.phoneEditText.setText("05$digitsOnly")
                            binding.phoneEditText.setSelection(binding.phoneEditText.length())
                        }
                    }

                    if (digitsOnly.length > 11) {
                        binding.phoneEditText.setText(digitsOnly.substring(0, 11))
                        binding.phoneEditText.setSelection(11)
                        showError("Telefon numarası 11 rakamdan fazla olamaz")
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        binding.registerButton.setOnClickListener {
            if (validateInputs()) {
                checkPhoneNumber()
            }
        }

        binding.privacyPolicyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.registerButton.isEnabled = isChecked
        }

        binding.readPrivacyPolicyButton.setOnClickListener {
            showPrivacyPolicyDialog()
        }
    }

    private fun showPrivacyPolicyDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Kişisel Verilerin Korunması ve Gizlilik Sözleşmesi")
            .setMessage(R.string.privacy_policy_text)
            .setPositiveButton("Kabul Ediyorum") { _, _ ->
                binding.privacyPolicyCheckBox.isChecked = true
            }
            .setNegativeButton("Reddet") { _, _ ->
                binding.privacyPolicyCheckBox.isChecked = false
            }
            .show()
    }

    private fun validateInputs(): Boolean {
        val firstName = binding.firstNameEditText.text.toString()
        val lastName = binding.lastNameEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
            password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Lütfen tüm zorunlu alanları doldurun")
            return false
        }

        if (password != confirmPassword) {
            showError("Şifreler eşleşmiyor")
            return false
        }

        if (!binding.privacyPolicyCheckBox.isChecked) {
            showError("Lütfen gizlilik sözleşmesini kabul edin")
            return false
        }

        return true
    }

    private fun checkPhoneNumber() {
        showLoading(true)
        val phone = binding.phoneEditText.text.toString()

        if (phone.isEmpty()) {
            registerUser()
            return
        }

        if (!phone.startsWith("05") || phone.length != 11) {
            showLoading(false)
            showError("Geçersiz telefon numarası formatı")
            return
        }

        db.collection("users")
            .whereEqualTo("phoneNumber", phone)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    showLoading(false)
                    showError("Bu telefon numarası zaten kayıtlı")
                    return@addOnSuccessListener
                }

                sendVerificationCode(phone)
            }
            .addOnFailureListener {
                showLoading(false)
                showError("Bir hata oluştu: ${it.message}")
            }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val formattedNumber = "+90${phoneNumber.substring(1)}"

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            formattedNumber,
            120,
            TimeUnit.SECONDS,
            this,
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    showLoading(false)
                    registerUser()
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    showLoading(false)
                    showError("Doğrulama başarısız: ${e.message}")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    showLoading(false)
                    this@RegisterActivity.verificationId = verificationId
                    showVerificationDialog()
                }
            }
        )
    }

    private fun showVerificationDialog() {
        VerificationDialogFragment.newInstance(verificationId!!) { isSuccess ->
            if (isSuccess) {
                registerUser()
            }
        }.show(supportFragmentManager, "verification_dialog")
    }

    private fun registerUser() {
        showLoading(true)

        auth.createUserWithEmailAndPassword(
            binding.emailEditText.text.toString(),
            binding.passwordEditText.text.toString()
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveUserDetails(task.result?.user?.uid)
            } else {
                showLoading(false)
                showError("Kayıt başarısız: ${task.exception?.message}")
            }
        }
    }

    private fun saveUserDetails(userId: String?) {
        if (userId == null) {
            showLoading(false)
            showError("Kullanıcı ID alınamadı")
            return
        }

        val userData = hashMapOf(
            "firstName" to binding.firstNameEditText.text.toString(),
            "lastName" to binding.lastNameEditText.text.toString(),
            "email" to binding.emailEditText.text.toString(),
            "phoneNumber" to binding.phoneEditText.text.toString(),
            "profession" to binding.professionEditText.text.toString(),
            "heardFrom" to binding.heardFromAutoComplete.text.toString()
        )

        binding.birthDateEditText.text?.toString()?.let {
            if (it.isNotEmpty()) {
                userData["birthDate"] = it
            }
        }

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Kullanıcı bilgileri kaydedilemedi: ${e.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.registerButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}