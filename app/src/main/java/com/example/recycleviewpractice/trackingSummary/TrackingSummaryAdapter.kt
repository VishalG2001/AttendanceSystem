package com.example.recycleviewpractice.trackingSummary

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.recycleviewpractice.R
import com.example.recycleviewpractice.databinding.ItemTrackingSummaryBinding

class TrackingSummaryAdapter(private val items: List<ModelTrackingSummary>) :
    RecyclerView.Adapter<TrackingSummaryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrackingSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(private val binding: ItemTrackingSummaryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ModelTrackingSummary) {
            binding.apply {
                tvTrackingDate.text = item.date
                tvStatus.text = item.status
                tvStartTrackingTime.text = item.startTime
                tvEndTrackingTime.text = item.endTime
                tvStartTrackingTitle.text = item.startTitle
                tvEndTrackingTitle.text = item.endTitle

                // Check if status is "Completed" and update UI accordingly
                when (item.status) {
                    "Completed" -> {
                        tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.hint_of_green))
                        tvStatus.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.eucalyptus)))
                        // Change start drawable color
                        val startDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_status)
                        startDrawable?.setColorFilter(ContextCompat.getColor(itemView.context, R.color.eucalyptus), PorterDuff.Mode.SRC_IN)
                        tvStatus.setCompoundDrawablesWithIntrinsicBounds(startDrawable, null, null, null)

                        ivEndTracking.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.eucalyptus))
                       vProgressBar.setLineColor(ContextCompat.getColor(itemView.context, R.color.eucalyptus))
                        ivStartTracking.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.eucalyptus))
                        ItemTrackingCard.strokeColor = ContextCompat.getColor(itemView.context, R.color.mercury) // Update stroke color to green
                    }
                    "Cancelled" -> {
                        tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.chablis))
                        tvStatus.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.thunderbird)))
                        // Change start drawable color
                        val startDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.ic_status)
                        startDrawable?.setColorFilter(ContextCompat.getColor(itemView.context, R.color.thunderbird), PorterDuff.Mode.SRC_IN)
                        tvStatus.setCompoundDrawablesWithIntrinsicBounds(startDrawable, null, null, null)
                        //change the date icon
                        ivEndTracking.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.thunderbird))
                        vProgressBar.setLineColor(ContextCompat.getColor(itemView.context, R.color.thunderbird))
                        ivStartTracking.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.thunderbird))
                        ItemTrackingCard.strokeColor = ContextCompat.getColor(itemView.context, R.color.mercury) // Update stroke color to red
                    }
                    else -> {
                        // Reset to default colors if status is not "Completed" or "Cancelled"
                        tvStatus.backgroundTintList = null
                        ivStartTracking.backgroundTintList = null
                        root.backgroundTintList=ColorStateList.valueOf(ContextCompat.getColor(itemView.context, R.color.quarter_pearl_lusta))
                        ivEndTracking.background = ContextCompat.getDrawable(itemView.context, R.drawable.blankstatus)
                        ItemTrackingCard.strokeColor = ContextCompat.getColor(itemView.context, R.color.buddha_gold)
                    }
                }
            }
        }
    }

}
