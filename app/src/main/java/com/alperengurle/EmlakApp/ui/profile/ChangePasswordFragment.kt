package com.alperengurle.EmlakApp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.alperengurle.EmlakApp.databinding.FragmentChangePasswordBinding
import com.alperengurle.EmlakApp.viewmodel.ChangePasswordViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.fragment.findNavController
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChangePasswordFragment : Fragment() {
    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChangePasswordViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            changePasswordButton.setOnClickListener {
                changePassword()
            }
        }
    }

    private fun changePassword() {
        val currentPassword = binding.currentPasswordEditText.text.toString()
        val newPassword = binding.newPasswordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()

        if (!validateInputs(currentPassword, newPassword, confirmPassword)) {
            return
        }

        val user = auth.currentUser
        if (user?.email == null) {
            showError("Kullanıcı bilgisi alınamadı")
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.changePasswordButton.isEnabled = false

        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                viewModel.changePassword(newPassword)
            }
            .addOnFailureListener {
                showError("Mevcut şifre yanlış")
                binding.progressBar.visibility = View.GONE
                binding.changePasswordButton.isEnabled = true
            }
    }

    private fun validateInputs(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Tüm alanları doldurun")
            return false
        }

        if (newPassword != confirmPassword) {
            showError("Yeni şifreler eşleşmiyor")
            return false
        }

        if (newPassword.length < 6) {
            showError("Şifre en az 6 karakter olmalı")
            return false
        }

        return true
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun observeViewModel() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.changePasswordButton.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}