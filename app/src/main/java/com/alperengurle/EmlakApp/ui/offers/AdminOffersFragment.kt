package com.alperengurle.EmlakApp.ui.offers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.databinding.FragmentOffersBinding
import com.alperengurle.EmlakApp.viewmodel.OffersViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdminOffersFragment : Fragment() {
    private var _binding: FragmentOffersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OffersViewModel by viewModels()
    private val args: AdminOffersFragmentArgs by navArgs()
    private lateinit var adapter: OffersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOffersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.loadListingOffers(args.listingId)
    }

    private fun setupRecyclerView() {
        adapter = OffersAdapter(
            isAdmin = true,
            onItemClick = { offerWithListing ->
                findNavController().navigate(
                    AdminOffersFragmentDirections.actionAdminOffersToListingDetail(
                        offerWithListing.listing.id!!
                    )
                )
            },
            onDeleteClick = { offerWithListing ->
                showDeleteConfirmationDialog(offerWithListing.offer.id)
            }
        )

        binding.offersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AdminOffersFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.offers.observe(viewLifecycleOwner) { offers ->
            adapter.submitList(offers)
            binding.emptyView.visibility = if (offers.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun showDeleteConfirmationDialog(offerId: String?) {
        if (offerId == null) return

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