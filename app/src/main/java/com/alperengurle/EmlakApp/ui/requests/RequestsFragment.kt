package com.alperengurle.EmlakApp.ui.requests

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.data.model.Request
import com.alperengurle.EmlakApp.databinding.FragmentRequestsBinding
import com.alperengurle.EmlakApp.ui.auth.LoginActivity
import com.alperengurle.EmlakApp.ui.home.ListingsAdapter
import com.alperengurle.EmlakApp.viewmodel.MatchingListingsViewModel
import com.alperengurle.EmlakApp.viewmodel.RequestsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class RequestsFragment : Fragment() {
    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RequestsViewModel by viewModels()
    private val matchingListingsViewModel: MatchingListingsViewModel by viewModels()
    private lateinit var adapter: RequestsAdapter
    private lateinit var matchingListingsAdapter: ListingsAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setupUI()
        // İlk açılışta talepleri yükle
        viewModel.loadRequests()
        loadMatchingListings()
        observeViewModel()
    }

    private fun observeViewModel() {
        // Talepleri dinle ve göster
        viewModel.requests.observe(viewLifecycleOwner) { requests ->
            if (requests.isNotEmpty()) {
                binding.requestsRecyclerView.isVisible = true
                adapter.submitList(requests)
            } else {
                binding.requestsRecyclerView.isVisible = false
            }
        }

        matchingListingsViewModel.matchingListings.observe(viewLifecycleOwner) { listings ->
            binding.matchingListingsRecyclerView.isVisible = listings.isNotEmpty()
            matchingListingsAdapter.updateListings(listings)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    private fun setupUI() {
        setupRecyclerView()
        setupFab()
    }

    private fun setupRecyclerView() {
        // Talepler adapter'ı
        adapter = RequestsAdapter(
            onDeleteClick = { request ->
                showDeleteConfirmationDialog(request)
            }
        )

        binding.requestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RequestsFragment.adapter
            setHasFixedSize(true)
        }

        // Eşleşen ilanlar adapter'ı
        // Eşleşen ilanlar adapter'ı
        matchingListingsAdapter = ListingsAdapter(
            emptyList(),
            onListingClick = { listing ->
                listing.id?.let { id ->
                    findNavController().navigate(
                        // Burada action_requests_to_listingDetail kullanıyoruz
                        RequestsFragmentDirections.actionRequestsToListingDetail(id)
                    )
                }
            },
            onFavoriteClick = { listing ->
                matchingListingsViewModel.toggleFavorite(listing)
            }
        )

        binding.matchingListingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = matchingListingsAdapter
            setHasFixedSize(true)
        }
    }

    private fun showDeleteConfirmationDialog(request: Request) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Talebi Sil")
            .setMessage("Bu talebi silmek istediğinize emin misiniz?")
            .setPositiveButton("Sil") { dialog, _ ->
                request.id?.let { requestId ->
                    viewModel.deleteRequest(requestId)
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    private fun setupFab() {
        binding.addRequestFab.setOnClickListener {
            if (auth.currentUser?.isAnonymous == true) {
                showLoginDialog()
            } else {
                findNavController().navigate(R.id.action_requests_to_addRequest)
            }
        }
    }

    private fun setupObservers() {
        viewModel.requests.observe(viewLifecycleOwner) { requests ->
            binding.requestsRecyclerView.isVisible = requests.isNotEmpty()
            adapter.submitList(requests)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    private fun loadMatchingListings() {
        viewModel.requests.observe(viewLifecycleOwner) { requests ->
            matchingListingsViewModel.clearMatchingListings() // Önce listeyi temizle
            requests.forEach { request ->
                request.id?.let { requestId ->
                    matchingListingsViewModel.loadMatchingListings(requestId)
                }
            }
        }
    }
    private fun navigateToMatchingListings(requestId: String) {
        try {
            matchingListingsViewModel.loadMatchingListings(requestId)
            findNavController().navigate(
                RequestsFragmentDirections.actionRequestsToMatchingListings(requestId)
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Eşleşen ilanlar yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoginDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Üyelik Gerekli")
            .setMessage("Bu özelliği kullanmak için üye olmanız gerekmektedir.")
            .setPositiveButton("Üye Ol") { _, _ ->
                requireActivity().apply {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRequests()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}