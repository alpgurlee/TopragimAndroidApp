package com.alperengurle.EmlakApp.ui.listing

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.data.model.ListingDetails
import com.alperengurle.EmlakApp.databinding.FragmentEditListingBinding
import com.alperengurle.EmlakApp.util.LocationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditListingFragment : Fragment() {
    private var _binding: FragmentEditListingBinding? = null
    private val binding get() = _binding!!
    private val args: EditListingFragmentArgs by navArgs()
    private lateinit var listing: Listing
    private lateinit var mediaAdapter: MediaAdapter

    private val selectedImages = mutableListOf<Uri>()
    private val selectedVideos = mutableListOf<Uri>()
    private val existingMediaUrls = mutableListOf<String>()

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Medya seçimi
    private val selectImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let {
            selectedImages.addAll(it)
            updateMediaAdapter()
        }
    }

    private val selectVideos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let {
            selectedVideos.addAll(it)
            updateMediaAdapter()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadListing()
    }

    private fun setupUI() {
        setupToolbar()
        setupMediaRecyclerView()
        setupLocationSpinners()
        setupStatusSpinners()
        setupUpdateButton() // Bunu ekleyin
    }
    private fun setupUpdateButton() {
        binding.updateButton.setOnClickListener {
            updateListing()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupMediaRecyclerView() {
        mediaAdapter = MediaAdapter { position ->
            removeMedia(position)
        }

        binding.mediaRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = mediaAdapter
        }

        binding.addMediaFab.setOnClickListener {
            showMediaPickerDialog()
        }
    }

    private fun setupLocationSpinners() {
        val cities = LocationUtils.getCities(requireContext())

        val cityAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            cities.map { it.name }
        )
        binding.cityAutoComplete.setAdapter(cityAdapter)

        binding.cityAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = cities[position]
            val districts = LocationUtils.getDistricts(requireContext(), selectedCity.id)

            val districtAdapter = ArrayAdapter(
                requireContext(),
                R.layout.dropdown_item,
                districts.map { it.name }
            )
            binding.districtAutoComplete.setAdapter(districtAdapter)
            binding.districtAutoComplete.text.clear()
            binding.neighborhoodAutoComplete.text.clear()
        }

        binding.districtAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = cities.first { it.name == binding.cityAutoComplete.text.toString() }
            val districts = LocationUtils.getDistricts(requireContext(), selectedCity.id)
            val selectedDistrict = districts[position]

            val neighborhoods = LocationUtils.getNeighborhoods(requireContext(), selectedDistrict.id)
            val neighborhoodAdapter = ArrayAdapter(
                requireContext(),
                R.layout.dropdown_item,
                neighborhoods.map { it.name }
            )
            binding.neighborhoodAutoComplete.setAdapter(neighborhoodAdapter)
            binding.neighborhoodAutoComplete.text.clear()
        }
    }

    private fun setupStatusSpinners() {
        // İmar durumu için adapter
        val zoningStatuses = arrayOf(
            "Ada", "A-Lejantlı", "Arazi", "Bağ & Bahçe", "Depo", "Eğitim",
            "Enerji Depolama", "Konut", "Muhtelif", "Özel Kullanım", "Sağlık",
            "Sanayi", "Sera", "Sit Alanı", "Spor Alanı", "Tarla", "Tarla + Bağ",
            "Ticari", "Ticari + Konut", "Toplu Konut", "Turizm", "Turizm + Konut",
            "Turizm + Ticari", "Villa", "Zeytinlik"
        )

        val zoningAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            zoningStatuses
        )
        binding.zoningStatusAutoComplete.setAdapter(zoningAdapter)

        // Tapu durumu için adapter
        val deedStatuses = arrayOf(
            "Hisseli Tapu", "Müstakil Tapu", "Tahsis Tapu",
            "Zilliyet Tapu", "Yurt Dışı Tapulu", "Tapu Kaydı Yok"
        )

        val deedAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            deedStatuses
        )
        binding.deedStatusAutoComplete.setAdapter(deedAdapter)
    }

    private fun loadListing() {
        showLoading(true)
        db.collection("listings").document(args.listingId)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Listing::class.java)?.let { listing ->
                    this.listing = listing
                    populateFields(listing)
                    loadExistingMedia(listing.mediaUrls)
                }
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showError("İlan yüklenirken hata oluştu: ${e.message}")
                showLoading(false)
            }
    }

    private fun populateFields(listing: Listing) {
        binding.apply {
            titleEditText.setText(listing.title)
            descriptionEditText.setText(listing.description)
            priceEditText.setText(listing.price.toString())

            cityAutoComplete.setText(listing.city)
            districtAutoComplete.setText(listing.district)
            neighborhoodAutoComplete.setText(listing.neighborhood)

            zoningStatusAutoComplete.setText(listing.details.zoningStatus)
            areaSizeEditText.setText(listing.details.areaSize)
            adaNoEditText.setText(listing.details.adaNo)
            parselNoEditText.setText(listing.details.parselNo)
            deedStatusAutoComplete.setText(listing.details.deedStatus)

            carTradeSwitch.isChecked = listing.details.carTrade ?: false
            houseTradeSwitch.isChecked = listing.details.houseTrade ?: false
        }
    }

    private fun loadExistingMedia(mediaUrls: List<String>?) {
        mediaUrls?.let {
            existingMediaUrls.addAll(it)
            updateMediaAdapter()
        }
    }

    private fun showMediaPickerDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Medya Ekle")
            .setItems(arrayOf("Fotoğraf Ekle", "Video Ekle")) { _, which ->
                when (which) {
                    0 -> selectImages.launch("image/*")
                    1 -> selectVideos.launch("video/*")
                }
            }
            .show()
    }

    private fun removeMedia(position: Int) {
        when {
            position < existingMediaUrls.size -> {
                existingMediaUrls.removeAt(position)
            }
            position < existingMediaUrls.size + selectedImages.size -> {
                selectedImages.removeAt(position - existingMediaUrls.size)
            }
            else -> {
                selectedVideos.removeAt(position - existingMediaUrls.size - selectedImages.size)
            }
        }
        updateMediaAdapter()
    }

    private fun updateMediaAdapter() {
        val allMedia = existingMediaUrls.map { Uri.parse(it) } + selectedImages + selectedVideos
        mediaAdapter.submitList(allMedia)
    }

    // UpdateListing fonksiyonunda değişiklik
    private fun updateListing() {
        showLoading(true)

        val updatedListing = listing.copy(
            title = binding.titleEditText.text.toString(),
            description = binding.descriptionEditText.text.toString(),
            price = binding.priceEditText.text.toString().toDoubleOrNull() ?: 0.0,
            city = binding.cityAutoComplete.text.toString(),
            district = binding.districtAutoComplete.text.toString(),
            neighborhood = binding.neighborhoodAutoComplete.text.toString(),
            details = ListingDetails(
                zoningStatus = binding.zoningStatusAutoComplete.text.toString(),
                areaSize = binding.areaSizeEditText.text.toString(),
                adaNo = binding.adaNoEditText.text.toString(),
                parselNo = binding.parselNoEditText.text.toString(),
                deedStatus = binding.deedStatusAutoComplete.text.toString(),
                carTrade = binding.carTradeSwitch.isChecked,
                houseTrade = binding.houseTradeSwitch.isChecked
            ),
            status = "pending",
            mediaUrls = existingMediaUrls // Boş olsa bile sorun olmayacak
        )

        uploadNewMedia(updatedListing)
    }

    // uploadNewMedia fonksiyonu düzeltildi
    private fun uploadNewMedia(updatedListing: Listing) {
        var remainingUploads = selectedImages.size + selectedVideos.size
        if (remainingUploads == 0) {
            saveListingToFirestore(updatedListing)
            return
        }

        val currentMediaUrls = updatedListing.mediaUrls.toMutableList()

        // Resimleri yükle
        selectedImages.forEach { uri ->
            val ref = storage.reference.child("listing_images/${System.currentTimeMillis()}.jpg")
            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    currentMediaUrls.add(downloadUri.toString())
                    remainingUploads--
                    if (remainingUploads == 0) {
                        updatedListing.mediaUrls = currentMediaUrls
                        saveListingToFirestore(updatedListing)
                    }
                }
                .addOnFailureListener {
                    showError("Medya yüklenirken hata oluştu")
                    showLoading(false)
                }
        }

        // Videoları yükle
        selectedVideos.forEach { uri ->
            val ref = storage.reference.child("listing_videos/${System.currentTimeMillis()}.mp4")
            ref.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    currentMediaUrls.add(downloadUri.toString())
                    remainingUploads--
                    if (remainingUploads == 0) {
                        updatedListing.mediaUrls = currentMediaUrls
                        saveListingToFirestore(updatedListing)
                    }
                }
                .addOnFailureListener {
                    showError("Video yüklenirken hata oluştu")
                    showLoading(false)
                }
        }
    }

    private fun saveListingToFirestore(listing: Listing) {
        listing.id?.let { id ->
            db.collection("listings").document(id)
                .set(listing)
                .addOnSuccessListener {
                    showLoading(false)
                    Toast.makeText(requireContext(), "İlan güncellendi", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showError("İlan güncellenirken hata oluştu: ${e.message}")
                }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}