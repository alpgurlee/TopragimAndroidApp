package com.alperengurle.EmlakApp.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.databinding.FragmentSearchBinding
import com.alperengurle.EmlakApp.ui.home.ListingsAdapter
import com.alperengurle.EmlakApp.util.LocationUtils
import com.alperengurle.EmlakApp.viewmodel.SearchViewModel

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var listingsAdapter: ListingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchViews()
        setupLocationPickers()
        setupPriceRangeSlider()
        setupCategorySelection()
        setupClearFilters()

        // ViewModel'ı observe et
        viewModel.searchResults.observe(viewLifecycleOwner) { listings ->
            listingsAdapter.updateListings(listings)
            binding.searchResultsRecyclerView.visibility =
                if (listings.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupSearchViews() {
        binding.searchEditText.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            viewModel.search(query)
        }
    }

    private fun setupLocationPickers() {
        val cities = LocationUtils.getCities(requireContext())

        binding.citySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Tüm İller") + cities.map { it.name }
        )

        binding.districtSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Tüm İlçeler")
        )

        binding.citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    viewModel.updateLocation(null, null)
                    return
                }

                val selectedCity = cities[position - 1]
                val districts = LocationUtils.getDistricts(requireContext(), selectedCity.id)

                binding.districtSpinner.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf("Tüm İlçeler") + districts.map { it.name }
                )

                viewModel.updateLocation(selectedCity.name, null)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.districtSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    viewModel.updateLocation(
                        binding.citySpinner.selectedItem.toString(),
                        null
                    )
                    return
                }

                viewModel.updateLocation(
                    binding.citySpinner.selectedItem.toString(),
                    parent?.getItemAtPosition(position).toString()
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCategorySelection() {
        binding.arsaCardView.setOnClickListener {
            viewModel.updateCategory("Arsa")
            updateCategorySelection("Arsa")
        }

        binding.tarlaCardView.setOnClickListener {
            viewModel.updateCategory("Tarla")
            updateCategorySelection("Tarla")
        }
    }

    private fun updateCategorySelection(selectedCategory: String?) {
        val arsaCard = binding.arsaCardView
        val tarlaCard = binding.tarlaCardView

        arsaCard.setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                if (selectedCategory == "Arsa") R.color.green_light else R.color.category_background
            )
        )

        tarlaCard.setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                if (selectedCategory == "Tarla") R.color.green_light else R.color.category_background
            )
        )
    }

    private fun setupPriceRangeSlider() {
        binding.priceRangeSlider.apply {
            valueTo = 10000000f
            valueFrom = 0f
            setValues(0f, 10000000f)
            addOnChangeListener { slider, _, _ ->
                val minPrice = slider.values[0].toLong()
                val maxPrice = slider.values[1].toLong()
                viewModel.updatePriceRange(minPrice, maxPrice)

                binding.minPriceText.text = "${formatPrice(minPrice)} TL"
                binding.maxPriceText.text = "${formatPrice(maxPrice)} TL"
            }
        }
    }

    private fun formatPrice(value: Long): String {
        return String.format("%,d", value)
    }

    private fun setupClearFilters() {
        binding.clearFiltersButton.setOnClickListener {
            // UI sıfırlama
            binding.searchEditText.text?.clear()
            binding.citySpinner.setSelection(0)
            binding.districtSpinner.setSelection(0)
            binding.priceRangeSlider.setValues(0f, 10000000f)
            updateCategorySelection(null)

            // ViewModel sıfırlama
            viewModel.clearAllFilters()
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { listings ->
            listingsAdapter.updateListings(listings)
            binding.searchResultsRecyclerView.visibility =
                if (listings.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupRecyclerView() {
        listingsAdapter = ListingsAdapter(
            listings = emptyList(),
            onListingClick = { listing ->
                listing.id?.let { id ->
                    findNavController().navigate(
                        SearchFragmentDirections.actionSearchToListingDetail(id)
                    )
                }
            },
            onFavoriteClick = { listing ->
                viewModel.toggleFavorite(listing)
            }
        )

        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listingsAdapter
            setHasFixedSize(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}