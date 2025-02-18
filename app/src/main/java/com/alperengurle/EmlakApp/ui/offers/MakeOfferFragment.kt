package com.alperengurle.EmlakApp.ui.offers

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.alperengurle.EmlakApp.data.model.Result
import com.alperengurle.EmlakApp.databinding.FragmentMakeOfferBinding
import com.alperengurle.EmlakApp.ui.auth.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MakeOfferFragment : Fragment() {
    private var _binding: FragmentMakeOfferBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MakeOfferViewModel by viewModels()
    private val args: MakeOfferFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMakeOfferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser?.isAnonymous == true) {
            showLoginDialog()
            return
        }

        setupViews()
        observeViewModel()
    }

    private fun showLoginDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Üyelik Gerekli")
            .setMessage("Teklif vermek için üye olmanız gerekmektedir.")
            .setPositiveButton("Üye Ol") { _, _ ->
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupViews() {
        // İlan fiyatını al
        FirebaseFirestore.getInstance()
            .collection("listings")
            .document(args.listingId)
            .get()
            .addOnSuccessListener { document ->
                val listingPrice = document.getDouble("price") ?: 0.0
                val minimumOffer = listingPrice * 0.60
               // binding.priceInfoText.text = "İlan Fiyatı: ${String.format("%,.0f ₺", listingPrice)}\nMinimum Teklif: ${String.format("%,.0f ₺", minimumOffer)}"
            }

        binding.submitButton.setOnClickListener {
            val offerAmount = binding.offerAmountEditText.text.toString().toDoubleOrNull()
            if (offerAmount != null) {
                viewModel.submitOffer(args.listingId, offerAmount)
            } else {
                showError("Lütfen geçerli bir teklif tutarı girin")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.offerResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    showSuccess("Teklifiniz başarıyla gönderildi")
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    showError(result.message)
                }
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}