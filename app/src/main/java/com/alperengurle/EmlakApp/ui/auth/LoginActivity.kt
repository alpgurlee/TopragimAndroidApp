package com.alperengurle.EmlakApp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alperengurle.EmlakApp.MainActivity
import com.alperengurle.EmlakApp.databinding.ActivityLoginBinding
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        callbackManager = CallbackManager.Factory.create()

        // Google Sign In yapılandırması
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("129553250713-pp1c7ndm9trii07aph27i9t8g4iv1hg0.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Kullanıcı zaten giriş yapmışsa ana ekrana yönlendir
        if (auth.currentUser != null) {
            startMainActivity()
            return
        }

        setupClickListeners()
        setupFacebookLogin()
    }

    private fun setupFacebookLogin() {
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    showError("Facebook girişi iptal edildi")
                }

                override fun onError(error: FacebookException) {
                    showError("Facebook girişi başarısız: ${error.message}")
                }
            })
    }

    private fun setupClickListeners() {
        binding.apply {
            loginButton.setOnClickListener {
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()

                if (validateInputs(email, password)) {
                    loginUser(email, password)
                }
            }

            registerText.setOnClickListener {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }

            binding.guestButton.setOnClickListener {
                showLoading(true) // Loading göstergesi ekleyin
                auth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        showLoading(false) // Loading göstergesini kaldırın
                        if (task.isSuccessful) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Ziyaretçi olarak giriş yaptınız. Bazı özellikler kısıtlıdır.",
                                Toast.LENGTH_LONG
                            ).show()
                            startMainActivity()
                        } else {
                            showError("Ziyaretçi girişi yapılamadı: ${task.exception?.message}")
                        }
                    }
            }

            forgotPasswordText.setOnClickListener {
                startActivity(Intent(this@LoginActivity, ResetPasswordActivity::class.java))
            }

            googleButton.setOnClickListener {
                signInWithGoogle()
            }

            facebookButton.setOnClickListener {
                LoginManager.getInstance().logInWithReadPermissions(
                    this@LoginActivity,
                    listOf("email", "public_profile")
                )
            }
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        showLoading(true)
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    showError("Facebook ile giriş başarısız: ${task.exception?.message}")
                }
                showLoading(false)
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Facebook Sign-In sonucu
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Google Sign-In sonucu
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showError("Google giriş başarısız: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    showError("Doğrulama başarısız: ${task.exception?.message}")
                }
                showLoading(false)
            }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.emailEditText.error = "E-posta adresi gerekli"
            return false
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Şifre gerekli"
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String) {
        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    showError(task.exception?.message ?: "Giriş başarısız")
                }
                showLoading(false)
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}