package com.billshare.app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.billshare.app.databinding.ItemSplitSummaryBinding
import com.billshare.app.models.SplitBill
import com.billshare.app.utils.DataManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SplitSummaryAdapter(
    private val bills: List<SplitBill>,
    private val onSettle: (SplitBill) -> Unit,
    private val onItemClick: (SplitBill) -> Unit = {},
    private val onPayerClick: (String) -> Unit = {},
    private val onShowDetails: (SplitBill) -> Unit = {}
) : RecyclerView.Adapter<SplitSummaryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSplitSummaryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSplitSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bill = bills[position]
        holder.binding.tvDescription.text = bill.description
        holder.binding.tvDetail.text = "${bill.paidBy.name} paid \$${"%.2f".format(bill.totalAmount)}"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.binding.tvDate.text = sdf.format(Date(bill.date))

        // clicking on payer name or detail navigates
        holder.binding.root.setOnClickListener { 
            // Show detailed information about who paid and who owes
            onShowDetails(bill)
        }
        holder.binding.tvDetail.setOnClickListener { onPayerClick(bill.paidBy.id) }

        // Check settlement status
        val context = holder.itemView.context
        val isFullySettled = DataManager.isBillFullySettled(context, bill)
        val currentUser = DataManager.getCurrentUser(context)
        val isCurrentUserSettled = currentUser != null && DataManager.isPersonSettledForBill(context, bill.id, currentUser.id)

        when {
            isFullySettled -> {
                holder.binding.tvStatus.text = "Settled ✓"
                holder.binding.tvStatus.setTextColor(Color.GRAY)
                holder.binding.btnSettle.isEnabled = false
                holder.binding.btnSettle.alpha = 0.4f
            }
            isCurrentUserSettled -> {
                // Current user has already settled their share
                holder.binding.tvStatus.text = "You settled ✓"
                holder.binding.tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                holder.binding.btnSettle.isEnabled = false
                holder.binding.btnSettle.alpha = 0.4f
            }
            else -> {
                // Current user can settle (either they owe or they have authority to settle)
                holder.binding.tvStatus.text = "Pending"
                holder.binding.tvStatus.setTextColor(Color.parseColor("#FF9800"))
                holder.binding.btnSettle.isEnabled = true
                holder.binding.btnSettle.alpha = 1.0f
                holder.binding.btnSettle.setOnClickListener { onSettle(bill) }
            }
        }
    }

    override fun getItemCount() = bills.size
}
