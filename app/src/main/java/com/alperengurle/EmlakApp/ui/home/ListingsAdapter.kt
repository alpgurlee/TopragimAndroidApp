package com.alperengurle.EmlakApp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.data.model.Listing
import com.alperengurle.EmlakApp.databinding.ItemListingBinding
import com.bumptech.glide.Glide

class ListingsAdapter(
    private var listings: List<Listing>,
    private val onListingClick: (Listing) -> Unit,
    private val onFavoriteClick: (Listing) -> Unit
) : RecyclerView.Adapter<ListingsAdapter.ListingViewHolder>() {

    inner class ListingViewHolder(
        private val binding: ItemListingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onListingClick(listings[position])
                }
            }

            binding.favoriteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFavoriteClick(listings[position])
                }
            }
        }

        fun bind(listing: Listing) {

            binding.apply {
                titleTextView.text = listing.title
                priceTextView.text = listing.formattedPrice
                locationTextView.text = "${listing.city}, ${listing.district}"
                categoryChip.text = listing.category

                if (listing.mediaUrls.isNotEmpty()) {
                    Glide.with(listingImageView)
                        .load(listing.mediaUrls[0])
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .centerCrop()
                        .into(listingImageView)
                } else {
                    Glide.with(listingImageView)
                        .load(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(listingImageView)
                }

                favoriteButton.setImageResource(
                    if (listing.isFavorited) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite_border
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        return ListingViewHolder(
            ItemListingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(listings[position])
    }

    override fun getItemCount(): Int = listings.size

    fun updateListings(newListings: List<Listing>) {
        listings = newListings
        notifyDataSetChanged()
    }
}