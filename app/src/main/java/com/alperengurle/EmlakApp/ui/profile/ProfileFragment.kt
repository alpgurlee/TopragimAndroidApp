package com.alperengurle.EmlakApp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.databinding.FragmentProfileBinding
import com.alperengurle.EmlakApp.ui.auth.LoginActivity
import com.alperengurle.EmlakApp.viewmodel.ProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (auth.currentUser?.isAnonymous == true) {
            setupGuestUI()
        } else {
            setupUI()
            observeViewModel()
        }
    }

    private fun setupGuestUI() {
        binding.apply {
            // Sadece başlık ve çıkış yap göster
            nameTextView.text = "Ziyaretçi"

            // Menü öğelerini gizle
            accountDetailsButton.isVisible = false
            changePasswordButton.isVisible = false
            myListingsButton.isVisible = false
            favoriteListingsButton.isVisible = false
            myOffersButton.isVisible = false
            settingsButton.isVisible = false
            helpButton.isVisible = false
            adminPanelButton.isVisible = false

            // Bilgilendirme mesajı ekle
            val infoText = TextView(requireContext()).apply {
                text = "Uygulamanın tüm özelliklerinden yararlanmak için üye olun"
                textSize = 16f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 32)
            }

            // Üye ol butonu
            val registerButton = MaterialButton(requireContext()).apply {
                text = "Üye Ol"
                setOnClickListener {
                    // Fragment yerine Activity'ye yönlendir
                    requireActivity().apply {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()  // Mevcut activity'yi kapat
                    }
                }
            }

            // Layout'a ekle
            val containerLayout = binding.root.findViewById<LinearLayout>(R.id.profile_container)
            containerLayout.addView(infoText)
            containerLayout.addView(registerButton)

            // Çıkış yap butonu
            logoutButton.setOnClickListener {
                auth.signOut()
                // Burada da Activity'ye yönlendir
                requireActivity().apply {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun setupUI() {
        checkIsAdmin()
        setupNavigationButtons()
    }

    private fun setupNavigationButtons() {
        binding.apply {
            adminPanelButton.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_adminPanel)
            }

            accountDetailsButton.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_accountDetails)
            }

            changePasswordButton.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_changePassword)
            }

            myListingsButton.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_myListings)
            }

            favoriteListingsButton.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_favorites)
            }

            myOffersButton.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_offers)
            }

            settingsButton.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_settings)
            }

            helpButton.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_help)
            }

            logoutButton.setOnClickListener {
                showLogoutDialog()
            }
        }
    }

    private fun checkIsAdmin() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                binding.adminPanelButton.isVisible = document.getString("role") == "admin"
            }
    }

    private fun observeViewModel() {
        viewModel.userDetails.observe(viewLifecycleOwner) { user ->
            binding.apply {
                nameTextView.text = "${user.firstName} ${user.lastName}"
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Çıkış Yap")
            .setMessage("Çıkış yapmak istediğinize emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hata")
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}