package com.alperengurle.EmlakApp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.alperengurle.EmlakApp.databinding.FragmentSettingsBinding
import com.alperengurle.EmlakApp.util.ThemePreferences
import com.alperengurle.EmlakApp.viewmodel.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            darkModeSwitch.isChecked = ThemePreferences.isDarkMode(requireContext())
            darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                ThemePreferences.setDarkMode(requireContext(), isChecked)
            }

            deleteAccountButton.setOnClickListener {
                showDeleteAccountDialog()
            }
        }
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hesabı Sil")
            .setMessage("Hesabınızı silmek istediğinize emin misiniz? Bu işlem geri alınamaz.")
            .setPositiveButton("Sil") { dialog, _ ->
                viewModel.deleteAccount()
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}