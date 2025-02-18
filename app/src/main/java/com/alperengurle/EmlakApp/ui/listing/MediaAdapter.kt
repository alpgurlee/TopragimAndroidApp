package com.alperengurle.EmlakApp.ui.listing

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alperengurle.EmlakApp.R
import com.alperengurle.EmlakApp.databinding.ItemMediaBinding
import com.bumptech.glide.Glide

class MediaAdapter(
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<Uri, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return MediaViewHolder(
            ItemMediaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onDeleteClick
        )
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MediaViewHolder(
        private val binding: ItemMediaBinding,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.deleteButton.setOnClickListener {
                onDeleteClick(absoluteAdapterPosition)
            }
        }

        fun bind(uri: Uri) {
            when {
                isImageFile(uri) -> {
                    Glide.with(binding.root)
                        .load(uri)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(binding.mediaImageView)
                    binding.videoIcon.visibility = View.GONE
                }
                isVideoFile(uri) -> {
                    Glide.with(binding.root)
                        .load(uri)
                        .placeholder(R.drawable.ic_video_placeholder)
                        .into(binding.mediaImageView)
                    binding.videoIcon.visibility = View.VISIBLE
                }
            }
        }

        private fun isImageFile(uri: Uri): Boolean {
            val mimeType = binding.root.context.contentResolver.getType(uri)
            return mimeType?.startsWith("image/") == true
        }

        private fun isVideoFile(uri: Uri): Boolean {
            val mimeType = binding.root.context.contentResolver.getType(uri)
            return mimeType?.startsWith("video/") == true
        }
    }

    class MediaDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * VideoThumbnailLoader sınıfını ister bu dosyada top-level olarak
 * (yani MediaAdapter kapanışının altında), ister ayrı bir dosyada tutabilirsiniz.
 */
class VideoThumbnailLoader(private val context: Context) {
    fun loadThumbnail(videoUri: Uri, imageView: ImageView) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val bitmap = retriever.getFrameAtTime(0)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
    }
}
