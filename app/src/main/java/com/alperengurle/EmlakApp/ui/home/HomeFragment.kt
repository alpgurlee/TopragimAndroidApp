package com.alperengurle.EmlakApp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.databinding.FragmentHomeBinding
import com.alperengurle.EmlakApp.viewmodel.HomeViewModel
import com.alperengurle.EmlakApp.viewmodel.SortType
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var listingsAdapter: ListingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        observeViewModel()
        viewModel.loadListings()
    }

    private fun setupUI() {
        binding.apply {
            // İlan Ver butonu
            addListingButton.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_addListing)
            }
            binding.sortButton.setOnClickListener {
                showSortingDialog()
            }
            // Bildirimler butonu
            /*notificationsButton.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_notifications)
            } */

            // Arama işlemi
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { viewModel.searchListings(it) }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { viewModel.searchListings(it) }
                    return true
                }
            })

            // Kategori seçimi için
            chipAll.setOnClickListener {
                viewModel.filterListings(null)
                chipAll.isChecked = true
                chipArsa.isChecked = false
                chipTarla.isChecked = false
            }
                // Bildirimler butonu ekle
            notificationsButton.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_notifications)
            }
            chipArsa.setOnClickListener {
                viewModel.filterListings("Arsa")
                chipAll.isChecked = false
                chipArsa.isChecked = true
                chipTarla.isChecked = false
            }

            chipTarla.setOnClickListener {
                viewModel.filterListings("Tarla")
                chipAll.isChecked = false
                chipArsa.isChecked = false
                chipTarla.isChecked = true
            }

            // Başlangıçta Tümü seçili olsun
            chipAll.isChecked = true
        }
    }
    private fun showSortingDialog() {
        val options = arrayOf(
            "Tarihe Göre En Yeni",
            "Tarihe Göre En Eski",
            "Fiyat (Azalan)",
            "Fiyat (Artan)"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sıralama Seçenekleri")
            .setItems(options) { dialog, which ->
                when(which) {
                    0 -> viewModel.sortListings(SortType.DATE_DESC)
                    1 -> viewModel.sortListings(SortType.DATE_ASC)
                    2 -> viewModel.sortListings(SortType.PRICE_DESC)
                    3 -> viewModel.sortListings(SortType.PRICE_ASC)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    private fun setupRecyclerView() {
        listingsAdapter = ListingsAdapter(
            listings = emptyList(),
            onListingClick = { listing ->
                listing.id?.let { id ->
                    findNavController().navigate(
                        HomeFragmentDirections.actionHomeToListingDetail(id)
                    )
                }
            },
            onFavoriteClick = { listing ->
                viewModel.toggleFavorite(listing)
            }
        )

        binding.listingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listingsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.listings.observe(viewLifecycleOwner) { listings ->
            listingsAdapter.updateListings(listings)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Hata durumunu göster
                // Snackbar veya Toast kullanabilirsiniz
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}