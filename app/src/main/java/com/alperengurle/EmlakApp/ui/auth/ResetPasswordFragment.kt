package com.alperengurle.EmlakApp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alperengurle.EmlakApp.databinding.FragmentResetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordFragment : Fragment() {
    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            requireContext(),
            "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi",
            Toast.LENGTH_LONG
        ).show()
        findNavController().navigateUp()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}