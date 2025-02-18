package com.alperengurle.EmlakApp.ui.listing

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.databinding.FragmentAddListingBinding
import com.alperengurle.EmlakApp.util.LocationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage


class AddListingFragment : Fragment() {

    private var _binding: FragmentAddListingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddListingViewModel by viewModels()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val selectedImages = mutableListOf<Uri>()
    private val selectedVideos = mutableListOf<Uri>()
    private lateinit var mediaAdapter: MediaAdapter

    // Resim seçme launcher
    private val selectImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let {
            selectedImages.addAll(it)
            mediaAdapter.submitList(selectedImages + selectedVideos)
        }
    }

    // Video seçme launcher
    private val selectVideos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let {
            selectedVideos.addAll(it)
            mediaAdapter.submitList(selectedImages + selectedVideos)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMediaRecyclerView()
        setupLocationSpinners()
        setupDetailSpinners()
        setupListeners()
        observeViewModel()
    }



    private fun setupMediaRecyclerView() {
        mediaAdapter = MediaAdapter(
            onDeleteClick = { position ->
                if (position < selectedImages.size) {
                    selectedImages.removeAt(position)
                } else {
                    selectedVideos.removeAt(position - selectedImages.size)
                }
                mediaAdapter.submitList(selectedImages + selectedVideos)
            }
        )

        binding.mediaRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mediaAdapter
        }

        binding.addMediaButton.setOnClickListener {
            showMediaPickerDialog()
        }
    }

    private fun setupLocationSpinners() {
        val cities = LocationUtils.getCities(requireContext())
        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cities.map { it.name })
        binding.cityAutoComplete.setAdapter(cityAdapter)

        binding.cityAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = cities[position]
            val districts = LocationUtils.getDistricts(requireContext(), selectedCity.id)
            val districtAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, districts.map { it.name })
            binding.districtAutoComplete.setAdapter(districtAdapter)
            binding.districtAutoComplete.text.clear()
            binding.neighborhoodAutoComplete.text.clear()
        }

        binding.districtAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = cities.first { it.name == binding.cityAutoComplete.text.toString() }
            val districts = LocationUtils.getDistricts(requireContext(), selectedCity.id)
            val selectedDistrict = districts[position]
            val neighborhoods = LocationUtils.getNeighborhoods(requireContext(), selectedDistrict.id)
            val neighborhoodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, neighborhoods.map { it.name })
            binding.neighborhoodAutoComplete.setAdapter(neighborhoodAdapter)
            binding.neighborhoodAutoComplete.text.clear()
        }
    }

    private fun setupDetailSpinners() {
        // İmar Durumu
        val zoningStatuses = arrayOf("Ada", "A-Lejantlı", "Arazi", "Bağ & Bahçe", "Depo", "Eğitim",
            "Enerji Depolama", "Konut", "Muhtelif", "Özel Kullanım", "Sağlık", "Sanayi", "Sera",
            "Sit Alanı", "Spor Alanı", "Tarla", "Tarla + Bağ", "Ticari", "Ticari + Konut",
            "Toplu Konut", "Turizm", "Turizm + Konut", "Turizm + Ticari", "Villa", "Zeytinlik")
        val zoningAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, zoningStatuses)
        binding.zoningStatusAutoComplete.setAdapter(zoningAdapter)

        // Tapu Durumu
        val deedStatuses = arrayOf("Hisseli Tapu", "Müstakil Tapu", "Tahsis Tapu", "Zilliyet Tapu", "Yurt Dışı Tapulu", "Tapu Kaydı Yok")
        val deedAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, deedStatuses)
        binding.deedStatusAutoComplete.setAdapter(deedAdapter)
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                saveListing()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val errorMessage = StringBuilder()

        if (selectedImages.isEmpty() && selectedVideos.isEmpty()) {
            errorMessage.append("En az bir fotoğraf veya video ekleyin\n")
            isValid = false
        }

        if (binding.titleEditText.text.isNullOrBlank()) {
            errorMessage.append("İlan başlığı boş olamaz\n")
            isValid = false
        }

        if (binding.priceEditText.text.isNullOrBlank()) {
            errorMessage.append("Fiyat boş olamaz\n")
            isValid = false
        }

        if (binding.cityAutoComplete.text.isNullOrBlank() ||
            binding.districtAutoComplete.text.isNullOrBlank() ||
            binding.neighborhoodAutoComplete.text.isNullOrBlank()) {
            errorMessage.append("Konum bilgileri eksik\n")
            isValid = false
        }

        if (!isValid) {
            Snackbar.make(binding.root, errorMessage.toString().trim(), Snackbar.LENGTH_LONG).show()
        }

        return isValid
    }

    private fun saveListing() {
        try {
            Log.d("AddListingFragment", "Starting to save listing")
            showLoading(true)

            val listing = createListingObject()
            Log.d("AddListingFragment", "Created listing object: $listing")

            viewModel.saveListing(listing, selectedImages, selectedVideos)

        } catch (e: Exception) {
            Log.e("AddListingFragment", "Error in saveListing", e)
            showLoading(false)
            Snackbar.make(binding.root, "Hata: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.saveButton.apply {
            isEnabled = !show
            text = if (show) "Yükleniyor..." else "İlanı Ekle"
        }
    }

    private fun createListingObject(): HashMap<String, Any> {
        return hashMapOf(
            "title" to binding.titleEditText.text.toString(),
            "description" to binding.descriptionEditText.text.toString(),
            "price" to binding.priceEditText.text.toString().toDouble(),
            "category" to if (binding.arsaRadioButton.isChecked) "Arsa" else "Tarla",
            "city" to binding.cityAutoComplete.text.toString(),
            "district" to binding.districtAutoComplete.text.toString(),
            "neighborhood" to binding.neighborhoodAutoComplete.text.toString(),
            "ownerID" to (auth.currentUser?.uid ?: ""),
            "status" to "pending",
            "details" to hashMapOf(
                "zoningStatus" to binding.zoningStatusAutoComplete.text.toString(),
                "areaSize" to binding.areaSizeEditText.text.toString(),
                "adaNo" to binding.adaNoEditText.text.toString(),
                "parselNo" to binding.parselNoEditText.text.toString(),
                "deedStatus" to binding.deedStatusAutoComplete.text.toString(),
                "carTrade" to binding.carTradeSwitch.isChecked,
                "houseTrade" to binding.houseTradeSwitch.isChecked,
                "parcelQueryLink" to binding.parselLinkEditText.text.toString()

            )
        )
    }

    private fun observeViewModel() {
        viewModel.listingSaved.observe(viewLifecycleOwner) { isSaved ->
            Log.d("AddListingFragment", "Listing saved: $isSaved")
            if (isSaved) {
                showLoading(false)
                Snackbar.make(binding.root, "İlanınız başarıyla eklendi ve onay için gönderildi", Snackbar.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Log.e("AddListingFragment", "Error in saving listing: $error")
            showLoading(false)
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showMediaPickerDialog() {
        val items = arrayOf("Fotoğraf Ekle", "Video Ekle")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Medya Ekle")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> selectImages.launch("image/*")
                    1 -> selectVideos.launch("video/*")
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}