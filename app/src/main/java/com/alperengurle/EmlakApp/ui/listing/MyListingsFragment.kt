package com.alperengurle.EmlakApp.ui.listing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.databinding.FragmentMyListingsBinding
import com.alperengurle.EmlakApp.viewmodel.MyListingsViewModel

class MyListingsFragment : Fragment() {
    private var _binding: FragmentMyListingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyListingsViewModel by viewModels()
    private lateinit var adapter: MyListingsAdapter // ListingsAdapter yerine MyListingsAdapter kullan

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyListingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.loadMyListings()
    }

    private fun setupRecyclerView() {
        adapter = MyListingsAdapter { listing ->
            listing.id?.let { id ->
                findNavController().navigate(
                    MyListingsFragmentDirections.actionMyListingsToListingDetail(id)
                )
            }
        }

        binding.listingsRecyclerView.apply {  // RecyclerView ID'si listingsRecyclerView olmalı
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyListingsFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.listings.observe(viewLifecycleOwner) { listings ->
            adapter.submitList(listings)  // updateListings yerine submitList kullan
            binding.emptyView.visibility = if (listings.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}