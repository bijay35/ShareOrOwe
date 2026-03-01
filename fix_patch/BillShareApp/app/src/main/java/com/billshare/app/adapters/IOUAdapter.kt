package com.billshare.app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.billshare.app.databinding.ItemIouBinding
import com.billshare.app.models.IOU

class IOUAdapter(
    private val ious: List<IOU>,
    private val onSettle: (IOU) -> Unit
) : RecyclerView.Adapter<IOUAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemIouBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIouBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val iou = ious[position]
        holder.binding.tvIouDescription.text = iou.description
        holder.binding.tvIouDetail.text = "${iou.paidBy.name} owes ${iou.owedTo.name} $" + "%.2f".format(iou.amount)

        if (iou.isSettled) {
            holder.binding.tvStatus.text = "Settled ✓"
            holder.binding.tvStatus.setTextColor(Color.GRAY)
            holder.binding.btnSettle.isEnabled = false
            holder.binding.btnSettle.alpha = 0.4f
        } else {
            holder.binding.tvStatus.text = "Pending"
            holder.binding.tvStatus.setTextColor(Color.parseColor("#FF9800"))
            holder.binding.btnSettle.isEnabled = true
            holder.binding.btnSettle.alpha = 1.0f
            holder.binding.btnSettle.setOnClickListener { onSettle(iou) }
        }
    }

    override fun getItemCount() = ious.size
}
