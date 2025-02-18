// app/src/main/java/com/alperengurle/EmlakApp/ui/notifications/NotificationsAdapter.kt
package com.alperengurle.EmlakApp.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alperengurle.EmlakApp.data.model.AppNotification
import com.alperengurle.EmlakApp.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsAdapter(
    private val onNotificationClick: (AppNotification) -> Unit,
    private val onDeleteClick: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: AppNotification) {
            binding.apply {
                iconImageView.setImageResource(notification.type.getIcon())
                iconImageView.setColorFilter(
                    ContextCompat.getColor(root.context, notification.type.getColor())
                )
                titleTextView.text = notification.title
                messageTextView.text = notification.message
                timeTextView.text = notification.timestamp.toDate().formatToString()

                root.alpha = if (notification.isRead) 0.7f else 1.0f
                root.setOnClickListener { onNotificationClick(notification) }
                deleteButton.setOnClickListener { onDeleteClick(notification) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return NotificationViewHolder(
            ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(oldItem: AppNotification, newItem: AppNotification) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AppNotification, newItem: AppNotification) =
            oldItem == newItem
    }

    private fun java.util.Date.formatToString(): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr"))
        return formatter.format(this)
    }
}