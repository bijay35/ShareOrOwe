package com.billshare.app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.billshare.app.databinding.ItemBalanceBinding
import com.billshare.app.models.Balance

class BalanceAdapter(private val balances: List<Balance>) :
    RecyclerView.Adapter<BalanceAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemBalanceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBalanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val balance = balances[position]
        holder.binding.tvPersonName.text = balance.person.name
        val amount = balance.netAmount
        when {
            amount > 0 -> {
                holder.binding.tvBalance.text = "Gets back \$${"%.2f".format(amount)}"
                holder.binding.tvBalance.setTextColor(Color.parseColor("#4CAF50"))
            }
            amount < 0 -> {
                holder.binding.tvBalance.text = "Owes \$${"%.2f".format(-amount)}"
                holder.binding.tvBalance.setTextColor(Color.parseColor("#F44336"))
            }
            else -> {
                holder.binding.tvBalance.text = "Settled ✓"
                holder.binding.tvBalance.setTextColor(Color.GRAY)
            }
        }
    }

    override fun getItemCount() = balances.size
}
