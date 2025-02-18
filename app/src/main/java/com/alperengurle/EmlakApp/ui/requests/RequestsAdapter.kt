package com.alperengurle.EmlakApp.ui.requests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alperengurle.EmlakApp.data.model.Request
import com.alperengurle.EmlakApp.databinding.ItemRequestBinding
import com.alperengurle.EmlakApp.util.formatPrice

class RequestsAdapter(
    private val onDeleteClick: (Request) -> Unit
) : ListAdapter<Request, RequestsAdapter.RequestViewHolder>(RequestDiffCallback()) {

    inner class RequestViewHolder(private val binding: ItemRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(request: Request) {
            binding.apply {
                cityTextView.text = request.city ?: "Tüm Şehirler"

                val priceRange = buildString {
                    append("Fiyat: ")
                    if (request.minPrice != null) {
                        append(request.minPrice.formatPrice())
                        if (request.maxPrice != null) {
                            append(" - ")
                            append(request.maxPrice.formatPrice())
                        }
                        append(" TL")
                    }
                }
                priceRangeTextView.text = priceRange
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RequestDiffCallback : DiffUtil.ItemCallback<Request>() {
        override fun areItemsTheSame(oldItem: Request, newItem: Request) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Request, newItem: Request) =
            oldItem == newItem
    }
}