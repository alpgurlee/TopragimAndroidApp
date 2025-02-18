package com.alperengurle.EmlakApp.ui.listing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.databinding.FragmentListingDetailBinding
import com.alperengurle.EmlakApp.ui.auth.LoginActivity
import com.alperengurle.EmlakApp.ui.listing.adapter.ListingImageAdapter
import com.alperengurle.EmlakApp.viewmodel.ListingDetailViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ListingDetailFragment : Fragment() {
    private var _binding: FragmentListingDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ListingDetailViewModel by viewModels()
    private lateinit var imageAdapter: ListingImageAdapter
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()

        arguments?.getString("listingId")?.let { listingId ->
            viewModel.loadListing(listingId)
        }
    }

    private fun setupViews() {
        setupToolbar()
        setupImageViewPager()
        setupButtons()
    }
    private fun showLoginDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Üyelik Gerekli")
            .setMessage("Teklif vermek için üye olmanız gerekmektedir.")
            .setPositiveButton("Üye Ol") { _, _ ->
                // Fragment yerine Activity'ye yönlendir
                requireActivity().apply {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            inflateMenu(R.menu.listing_detail_menu)
            setOnMenuItemClickListener { item ->
                when(item.itemId) {
                    R.id.action_share -> {
                        shareListing()
                        true
                    }
                    R.id.action_favorite -> {
                        viewModel.toggleFavorite()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupImageViewPager() {
        imageAdapter = ListingImageAdapter { position ->
            viewModel.currentListing.value?.let { listing ->
                val intent = Intent(requireContext(), FullScreenImageActivity::class.java).apply {
                    putStringArrayListExtra("imageUrls", ArrayList(listing.mediaUrls))
                    putExtra("position", position)
                }
                startActivity(intent)
            }
        }
        binding.imageViewPager.adapter = imageAdapter

        TabLayoutMediator(binding.imageIndicator, binding.imageViewPager) { _, _ ->
        }.attach()
    }

    private fun setupButtons() {
        binding.apply {
            makeOfferButton.setOnClickListener {
                if (auth.currentUser?.isAnonymous == true) {
                    showLoginDialog()
                } else {
                    viewModel.currentListing.value?.id?.let { listingId ->
                        if (listingId == auth.currentUser?.uid) {
                            showError("Kendi ilanınıza teklif veremezsiniz")
                        } else {
                            navigateToMakeOffer(listingId)
                        }
                    }
                }
            }

            showOffersButton.setOnClickListener {
                viewModel.currentListing.value?.id?.let { listingId ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        if (viewModel.isAdmin()) {
                            findNavController().navigate(
                                ListingDetailFragmentDirections.actionToAdminOffersView(listingId)
                            )
                        } else {
                            findNavController().navigate(
                                ListingDetailFragmentDirections.actionToUserOffersView(listingId)
                            )
                        }
                    }
                }
            }

            parselButton.setOnClickListener {
                viewModel.currentListing.value?.details?.parcelQueryLink ?.let { link ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    startActivity(intent)
                }
            }

            whatsappButton.setOnClickListener {
                openWhatsApp()
            }

            editButton.setOnClickListener {
                viewModel.currentListing.value?.id?.let { listingId ->
                    findNavController().navigate(
                        ListingDetailFragmentDirections.actionToEditListing(listingId)
                    )
                }
            }

            deleteButton.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    if (viewModel.canModifyListing()) {
                        showDeleteConfirmation()
                    }
                }    }
        }
    }

    private fun openWhatsApp() {
        val phoneNumber = "905333165520"  // Sabit WhatsApp numarası
        viewModel.currentListing.value?.let { listing ->
            val message = "Merhaba, ${listing.title} ilanı hakkında bilgi almak istiyorum."
            val encodedMessage = Uri.encode(message)

            val whatsappUri = Uri.parse("whatsapp://send?phone=$phoneNumber&text=$encodedMessage")
            val webWhatsappUri = Uri.parse("https://wa.me/$phoneNumber?text=$encodedMessage")

            try {
                startActivity(Intent(Intent.ACTION_VIEW, whatsappUri))
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, webWhatsappUri))
                } catch (e: Exception) {
                    showError("WhatsApp yüklenemedi")
                }
            }
        }
    }

    private fun shareListing() {
        viewModel.currentListing.value?.let { listing ->
            val shareText = buildString {
                append(listing.title)
                append("\nFiyat: ${listing.formattedPrice}")
                append("\nKonum: ${listing.city}, ${listing.district}")
                append("\nİlan No: ${listing.listingNumber}")
                append("\n\nTOPRAĞIM uygulamasından paylaşıldı")
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "İlanı Paylaş"))
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("İlanı Sil")
            .setMessage("Bu ilanı silmek istediğinizden emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                // Coroutine içinde deleteListing çağrısı
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteListing {
                        findNavController().navigateUp()
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun navigateToMakeOffer(listingId: String) {
        findNavController().navigate(
            ListingDetailFragmentDirections.actionListingDetailToMakeOffer(listingId)
        )
    }

    private fun showLoginRequired() {
        Snackbar.make(binding.root, "Teklif vermek için giriş yapmalısınız", Snackbar.LENGTH_LONG)
            .setAction("Giriş Yap") {
                findNavController().navigate(
                    ListingDetailFragmentDirections.actionListingDetailToLogin()
                )
            }.show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun observeViewModel() {
        viewModel.currentListing.observe(viewLifecycleOwner) { listing ->
            updateUI(listing)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.isFavorite.observe(viewLifecycleOwner) { isFavorite ->
            binding.toolbar.menu.findItem(R.id.action_favorite)?.setIcon(
                if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
            )
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    private fun updateUI(listing: Listing) {
        viewLifecycleOwner.lifecycleScope.launch {
            with(binding) {
                // Debug logs
                Log.d("ListingDetail", "Full Listing Object: $listing")
                Log.d("ListingDetail", "Details Object: ${listing.details}")
                Log.d("ListingDetail", "parcelQueryLink: ${listing.details?.parcelQueryLink}")

                // Temel bilgiler
                titleTextView.text = listing.title
                priceTextView.text = listing.formattedPrice
                listingNumberTextView.text = listing.listingNumber.toString()
                categoryTextView.text = "${listing.category} / ${listing.subCategory}"
                locationTextView.text = "${listing.city}, ${listing.district}"
                descriptionTextView.text = listing.description

                // Detay bilgileri
                zoningStatusTextView.text = listing.details?.zoningStatus ?: "-"
                areaSizeTextView.text = listing.details?.areaSize?.let { "$it m²" } ?: "-"
                adaNoTextView.text = listing.details?.adaNo ?: "-"
                parselNoTextView.text = listing.details?.parselNo ?: "-"
                deedStatusTextView.text = listing.details?.deedStatus ?: "-"
                carTradeTextView.text = if (listing.details?.carTrade == true) "Evet" else "Hayır"
                houseTradeTextView.text = if (listing.details?.houseTrade == true) "Evet" else "Hayır"

                // Medya
                if (listing.mediaUrls.isNotEmpty()) {
                    imageAdapter.submitList(listing.mediaUrls)
                }

                // Yetkiler
                val isAdmin = viewModel.isAdmin()
                val isOwner = listing.ownerID == auth.currentUser?.uid

                // Buton görünürlükleri
                makeOfferButton.isVisible = !isOwner && auth.currentUser != null
                editButton.isVisible = isOwner || isAdmin
                deleteButton.isVisible = isOwner || isAdmin
                ownerCard.isVisible = isAdmin

                // Parsel butonu kontrolü
                Log.d("ListingDetail", "Checking parcelQueryLink: ${listing.details?.parcelQueryLink}")
                parselButton.isVisible = !listing.details?.parcelQueryLink.isNullOrEmpty()

                if (parselButton.isVisible) {
                    Log.d("ListingDetail", "Parsel button should be visible")
                    parselButton.setOnClickListener {
                        listing.details?.parcelQueryLink?.let { link ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        }
                    }
                }

                // Admin için owner detayları
                if (isAdmin) {
                    loadOwnerDetails(listing.ownerID)
                }
            }
        }
    }

    private fun loadOwnerDetails(ownerId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val document = db.collection("users").document(ownerId)
                    .get()
                    .await()

                if (document != null) {
                    binding.ownerNameTextView.text = "${document.getString("firstName")} ${document.getString("lastName")}"
                    binding.ownerEmailTextView.text = document.getString("email")
                    binding.ownerPhoneTextView.text = document.getString("phoneNumber")
                }
            } catch (e: Exception) {
                showError("Kullanıcı bilgileri alınamadı")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}