package com.alperengurle.EmlakApp.ui.offers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.databinding.FragmentOffersBinding
import com.alperengurle.EmlakApp.viewmodel.OffersViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class OffersFragment : Fragment() {
    private var _binding: FragmentOffersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OffersViewModel by viewModels()
    private lateinit var adapter: OffersAdapter
    private var isLoading = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOffersBinding.inflate(inflater, container, false)
        Log.d("OffersFragment", "onCreateView called")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("OffersFragment", "onViewCreated called")

        setupRecyclerView()
        observeViewModel()

        // Ekranı yenilemeyi tetikle
        viewModel.loadOffers()
    }


    private fun setupRecyclerView() {
        Log.d("OffersFragment", "Setting up RecyclerView")
        binding.offersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        // Adapter'ı başlat
        adapter = OffersAdapter(
            isAdmin = false,
            onItemClick = { offerWithListing ->
                findNavController().navigate(
                    OffersFragmentDirections.actionOffersToListingDetail(
                        offerWithListing.listing.id!!
                    )
                )
            },
            onDeleteClick = { offerWithListing ->
                offerWithListing.offer.id?.let { offerId ->
                    showDeleteConfirmationDialog(offerId)
                }
            }
        )

        binding.offersRecyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        Log.d("OffersFragment", "Setting up observers")

        viewModel.offers.observe(viewLifecycleOwner) { offers ->
            Log.d("OffersFragment", "Received ${offers.size} offers")
            adapter.submitList(offers)

            // Görünürlüğü ayarla
            binding.emptyView.visibility = if (offers.isEmpty()) View.VISIBLE else View.GONE
            binding.offersRecyclerView.visibility = if (offers.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            Log.d("OffersFragment", "Loading state changed: $loading")
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.swipeRefreshLayout.isRefreshing = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e("OffersFragment", "Error received: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        Log.d("OffersFragment", "onResume called")
        viewModel.loadOffers() // Ekran her görünür olduğunda yenile
    }

    private fun showDeleteConfirmationDialog(offerId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Teklifi Sil")
            .setMessage("Bu teklifi silmek istediğinize emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                viewModel.deleteOffer(offerId)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}