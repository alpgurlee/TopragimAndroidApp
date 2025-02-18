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
import com.google.android.material.snackbar.Snackbar

class UserOffersFragment : Fragment() {
    private var _binding: FragmentOffersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OffersViewModel by viewModels()
    private val args: UserOffersFragmentArgs by navArgs()
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
            isAdmin = false,
            onItemClick = { offerWithListing ->
                findNavController().navigate(
                    UserOffersFragmentDirections.actionUserOffersToListingDetail(
                        offerWithListing.listing.id!!
                    )
                )
            },
            onDeleteClick = { offerWithListing ->
                // Normal kullanıcılar teklifleri silemez
            }
        )

        binding.offersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UserOffersFragment.adapter
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

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}