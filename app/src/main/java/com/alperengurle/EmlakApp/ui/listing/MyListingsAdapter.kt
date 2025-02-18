package com.alperengurle.EmlakApp.ui.listing

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.databinding.ItemMyListingBinding
import com.bumptech.glide.Glide
import com.alperengurle.EmlakApp.R

class MyListingsAdapter(
    private val onListingClick: (Listing) -> Unit
) : ListAdapter<Listing, MyListingsAdapter.ViewHolder>(ListingDiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemMyListingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.apply {
                titleTextView.text = listing.title
                priceTextView.text = listing.formattedPrice

                // Status text ve stilini ayarla
                val (statusText, backgroundColor, textColor) = when(listing.status) {
                    "approved" -> Triple(
                        "Onaylandı",
                        Color.parseColor("#E8F5E9"),
                        Color.parseColor("#2E7D32")
                    )
                    "pending" -> Triple(
                        "Onay Bekliyor",
                        Color.parseColor("#FFF3E0"),
                        Color.parseColor("#F57C00")
                    )
                    "rejected" -> Triple(
                        "Reddedildi",
                        Color.parseColor("#FFEBEE"),
                        Color.parseColor("#C62828")
                    )
                    else -> Triple(
                        listing.status,
                        Color.LTGRAY,
                        Color.GRAY
                    )
                }

                statusTextView.text = statusText
                statusTextView.setBackgroundColor(backgroundColor)
                statusTextView.setTextColor(textColor)

                // İlk medya öğesini yükle
                listing.mediaUrls.firstOrNull()?.let { imageUrl ->
                    Glide.with(listingImageView)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(listingImageView)
                } ?: run {
                    // Eğer medya yoksa placeholder göster
                    Glide.with(listingImageView)
                        .load(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(listingImageView)
                }

                // Tıklama olayı
                root.setOnClickListener { onListingClick(listing) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMyListingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class ListingDiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Listing, newItem: Listing) =
            oldItem == newItem
    }
}