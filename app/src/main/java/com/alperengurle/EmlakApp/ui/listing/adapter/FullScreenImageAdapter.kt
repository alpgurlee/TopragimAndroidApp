package com.alperengurle.EmlakApp.ui.listing.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.recyclerview.widget.RecyclerView
import com.alperengurle.EmlakApp.databinding.ItemFullscreenImageBinding
import com.bumptech.glide.Glide

// FullScreenImageAdapter.kt
// FullScreenImageAdapter.kt
class FullScreenImageAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<FullScreenImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemFullscreenImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(url: String) {
            if (isVideoUrl(url)) {
                binding.photoView.visibility = View.GONE
                binding.videoView.visibility = View.VISIBLE

                binding.videoView.apply {
                    setVideoURI(Uri.parse(url))
                    val mediaController = MediaController(context)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)
                    requestFocus()

                    setOnPreparedListener { mp ->
                        val displayMetrics = context.resources.displayMetrics
                        val videoWidth = mp.videoWidth
                        val videoHeight = mp.videoHeight
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels

                        val widthRatio = screenWidth.toFloat() / videoWidth
                        val heightRatio = screenHeight.toFloat() / videoHeight
                        val scale = kotlin.math.min(widthRatio, heightRatio)

                        val params = layoutParams
                        params.width = (videoWidth * scale).toInt()
                        params.height = (videoHeight * scale).toInt()
                        layoutParams = params

                        mp.isLooping = true
                        start()
                    }
                }
            } else {
                binding.photoView.visibility = View.VISIBLE
                binding.videoView.visibility = View.GONE

                Glide.with(binding.root)
                    .load(url)
                    .into(binding.photoView)
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
            ItemFullscreenImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUrls[position])
    }

    override fun getItemCount() = imageUrls.size
}