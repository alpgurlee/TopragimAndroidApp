package com.alperengurle.EmlakApp.ui.offers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alperengurle.EmlakApp.databinding.ItemOfferBinding
import com.alperengurle.EmlakApp.viewmodel.OffersViewModel.OfferWithListing
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale
import androidx.core.view.isVisible

class OffersAdapter(
    private val isAdmin: Boolean,
    private val onItemClick: (OfferWithListing) -> Unit,
    private val onDeleteClick: (OfferWithListing) -> Unit
) : ListAdapter<OfferWithListing, OffersAdapter.OfferViewHolder>(OfferDiffCallback()) {

    inner class OfferViewHolder(private val binding: ItemOfferBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(offerWithListing: OfferWithListing) {
            val (offer, listing) = offerWithListing
            binding.apply {
                // Temel bilgiler (herkes görebilir)
                titleTextView.text = listing.title
                priceTextView.text = "${formatPrice(offer.offerPrice)} TL"
                listingPriceTextView.text = "İlan Fiyatı: ${listing.formattedPrice}"
                dateTextView.text = formatDate(offer.timestamp.toDate())

                // Resim
                listing.mediaUrls.firstOrNull()?.let { imageUrl ->
                    Glide.with(imageView)
                        .load(imageUrl)
                        .centerCrop()
                        .into(imageView)
                }

                // Admin özel alanları
                userInfoContainer.isVisible = isAdmin
                if (isAdmin) {
                    userNameTextView.text = offer.userName
                    userEmailTextView.text = offer.userEmail
                    userPhoneTextView.text = offer.userPhone
                }

                // Silme butonu sadece admin görebilir
                deleteButton.isVisible = isAdmin
            }
        }

        private fun formatPrice(price: Double): String {
            return NumberFormat.getNumberInstance(Locale("tr", "TR")).apply {
                maximumFractionDigits = 0
            }.format(price)
        }

        private fun formatDate(date: java.util.Date): String {
            val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))
            return formatter.format(date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        return OfferViewHolder(
            ItemOfferBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class OfferDiffCallback : DiffUtil.ItemCallback<OfferWithListing>() {
        override fun areItemsTheSame(oldItem: OfferWithListing, newItem: OfferWithListing): Boolean {
            return oldItem.offer.id == newItem.offer.id
        }

        override fun areContentsTheSame(oldItem: OfferWithListing, newItem: OfferWithListing): Boolean {
            return oldItem == newItem
        }
    }
}