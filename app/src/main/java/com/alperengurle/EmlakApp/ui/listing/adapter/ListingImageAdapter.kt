package com.alperengurle.EmlakApp.ui.listing.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.alperengurle.EmlakApp.databinding.ItemListingImageBinding

class ListingImageAdapter(
    private val onImageClick: (Int) -> Unit
) : ListAdapter<String, ListingImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    inner class ImageViewHolder(
        private val binding: ItemListingImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onImageClick(absoluteAdapterPosition)
            }
        }

        fun bind(url: String) {
            if (isVideoUrl(url)) {
                binding.videoView.visibility = View.VISIBLE
                binding.imageView.visibility = View.GONE
                binding.loadingIndicator.visibility = View.VISIBLE

                binding.videoView.setVideoURI(Uri.parse(url))
                binding.videoView.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    binding.loadingIndicator.visibility = View.GONE
                    mediaPlayer.start()
                }

                binding.videoView.setOnErrorListener { _, _, _ ->
                    binding.loadingIndicator.visibility = View.GONE
                    binding.videoView.visibility = View.GONE
                    binding.imageView.visibility = View.VISIBLE
                    // Video yüklenemezse thumbnail göster
                    Glide.with(binding.root)
                        .load(url)
                        .into(binding.imageView)
                    true
                }
            } else {
                binding.videoView.visibility = View.GONE
                binding.imageView.visibility = View.VISIBLE
                binding.loadingIndicator.visibility = View.GONE

                Glide.with(binding.root)
                    .load(url)
                    .into(binding.imageView)
            }
        }

        private fun isVideoUrl(url: String): Boolean {
            return url.contains(".mp4", ignoreCase = true) ||
                    url.contains(".mov", ignoreCase = true) ||
                    url.contains("video", ignoreCase = true)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(
            ItemListingImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class ImageDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}