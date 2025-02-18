package com.alperengurle.EmlakApp.ui.listing

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.alperengurle.EmlakApp.databinding.ActivityFullScreenImageBinding
import com.alperengurle.EmlakApp.ui.listing.adapter.FullScreenImageAdapter

class FullScreenImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullScreenImageBinding
    private lateinit var imageAdapter: FullScreenImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrls = intent.getStringArrayListExtra("imageUrls") ?: ArrayList()
        val initialPosition = intent.getIntExtra("position", 0)

        setupViewPager(imageUrls, initialPosition)
        setupToolbar()
    }

    // Aşağıdaki sınıfın import'ları eklendi
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

    private fun setupViewPager(imageUrls: ArrayList<String>, initialPosition: Int) {
        imageAdapter = FullScreenImageAdapter(imageUrls)
        binding.viewPager.adapter = imageAdapter
        binding.viewPager.setCurrentItem(initialPosition, false)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}
