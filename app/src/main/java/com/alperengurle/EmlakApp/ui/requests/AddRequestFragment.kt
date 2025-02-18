package com.alperengurle.EmlakApp.ui.requests

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.alperengurle.EmlakApp.databinding.FragmentAddRequestBinding
import com.alperengurle.EmlakApp.util.City
import com.alperengurle.EmlakApp.util.District
import com.alperengurle.EmlakApp.util.LocationUtils
import com.alperengurle.EmlakApp.util.Neighborhood
import com.alperengurle.EmlakApp.viewmodel.AddRequestViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddRequestFragment : Fragment() {
    private var _binding: FragmentAddRequestBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddRequestViewModel by viewModels()

    private var selectedCityId: String? = null
    private var selectedDistrictId: String? = null
    private var selectedCity: String? = null
    private var selectedDistrict: String? = null
    private var selectedNeighborhood: String? = null
    private var selectedCategory: String? = null

    private var cities: List<City> = emptyList()
    private var districts: List<District> = emptyList()
    private var neighborhoods: List<Neighborhood> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCities()
        setupViews()
        setupObservers()
    }

    private fun loadCities() {
        cities = LocationUtils.getCities(requireContext())
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.cityCardView.setOnClickListener {
            showCityPickerDialog()
        }

        binding.districtCardView.setOnClickListener {
            if (selectedCityId == null) {
                Toast.makeText(requireContext(), "Lütfen önce şehir seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showDistrictPickerDialog()
        }

        binding.neighborhoodCardView.setOnClickListener {
            if (selectedDistrictId == null) {
                Toast.makeText(requireContext(), "Lütfen önce ilçe seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showNeighborhoodPickerDialog()
        }

        binding.categoryCardView.setOnClickListener {
            showCategoryPickerDialog()
        }

        binding.saveButton.setOnClickListener {
            saveRequest()
        }
    }

    private fun setupObservers() {
        viewModel.requestSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(requireContext(), "Talep başarıyla oluşturuldu", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.saveButton.isEnabled = !isLoading
            binding.progressCircular.isVisible = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun showCityPickerDialog() {
        val cityNames = cities.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Şehir Seçin")
            .setItems(cityNames) { _, which ->
                val city = cities[which]
                selectedCityId = city.id
                selectedCity = city.name
                binding.cityTextView.text = city.name

                // Reset district and neighborhood selections
                selectedDistrictId = null
                selectedDistrict = null
                selectedNeighborhood = null
                binding.districtTextView.text = "İlçe"
                binding.neighborhoodTextView.text = "Mahalle"

                // Load districts
                districts = LocationUtils.getDistricts(requireContext(), city.id)
            }
            .show()
    }

    private fun showDistrictPickerDialog() {
        val districtNames = districts.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("İlçe Seçin")
            .setItems(districtNames) { _, which ->
                val district = districts[which]
                selectedDistrictId = district.id
                selectedDistrict = district.name
                binding.districtTextView.text = district.name

                // Reset neighborhood selection
                selectedNeighborhood = null
                binding.neighborhoodTextView.text = "Mahalle"

                // Load neighborhoods
                neighborhoods = LocationUtils.getNeighborhoods(requireContext(), district.id)
            }
            .show()
    }

    private fun showNeighborhoodPickerDialog() {
        val neighborhoodNames = neighborhoods.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mahalle Seçin")
            .setItems(neighborhoodNames) { _, which ->
                selectedNeighborhood = neighborhoods[which].name
                binding.neighborhoodTextView.text = selectedNeighborhood
            }
            .show()
    }

    private fun showCategoryPickerDialog() {
        val categories = arrayOf("Arsa", "Tarla")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Kategori Seçin")
            .setItems(categories) { _, which ->
                selectedCategory = categories[which]
                binding.categoryTextView.text = selectedCategory
            }
            .show()
    }

    private fun saveRequest() {
        try {
            Log.d("AddRequestFragment", "Starting saveRequest")
            val minPrice = binding.minPriceEditText.text.toString().toDoubleOrNull()
            val maxPrice = binding.maxPriceEditText.text.toString().toDoubleOrNull()

            if (selectedCity == null) {
                Toast.makeText(requireContext(), "Lütfen şehir seçin", Toast.LENGTH_SHORT).show()
                return
            }

            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                Toast.makeText(requireContext(), "Minimum fiyat maksimum fiyattan büyük olamaz", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("AddRequestFragment", "Calling viewModel.saveRequest with: city=$selectedCity, district=$selectedDistrict")
            viewModel.saveRequest(
                city = selectedCity,
                district = selectedDistrict,
                neighborhood = selectedNeighborhood,
                minPrice = minPrice,
                maxPrice = maxPrice,
                category = selectedCategory
            )
        } catch (e: Exception) {
            Log.e("AddRequestFragment", "Error in saveRequest", e)
            Toast.makeText(requireContext(), "Bir hata oluştu: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}