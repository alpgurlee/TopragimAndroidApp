package com.alperengurle.EmlakApp.ui.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.databinding.FragmentMatchingListingsBinding
import com.alperengurle.EmlakApp.ui.home.ListingsAdapter
import com.alperengurle.EmlakApp.viewmodel.MatchingListingsViewModel
import androidx.navigation.fragment.findNavController


class MatchingListingsFragment : Fragment() {
    private var _binding: FragmentMatchingListingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MatchingListingsViewModel by viewModels()
    private val args: MatchingListingsFragmentArgs by navArgs()
    private lateinit var adapter: ListingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchingListingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        loadMatchingListings()
    }

    private fun setupRecyclerView() {
        adapter = ListingsAdapter(
            listings = emptyList(),
            onListingClick = { listing ->
                listing.id?.let { id ->
                    findNavController().navigate(
                        MatchingListingsFragmentDirections.actionMatchingListingsToListingDetail(id)
                    )
                }
            },
            onFavoriteClick = { listing ->
                viewModel.toggleFavorite(listing)
            }
        )
    }

    private fun setupObservers() {
        viewModel.matchingListings.observe(viewLifecycleOwner) { listings ->
            adapter.updateListings(listings)
            binding.emptyView.isVisible = listings.isEmpty()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.recyclerView.isVisible = !isLoading
        }
    }

    private fun loadMatchingListings() {
        viewModel.loadMatchingListings(args.requestId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}