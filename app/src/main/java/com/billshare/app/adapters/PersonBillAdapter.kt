package com.billshare.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.billshare.app.databinding.ItemPersonBillBinding
import com.billshare.app.models.SplitBill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PersonBillAdapter(private val bills: List<SplitBill>) :
    RecyclerView.Adapter<PersonBillAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPersonBillBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPersonBillBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bill = bills[position]
        holder.binding.tvDescription.text = bill.description
        holder.binding.tvAmount.text = "\$${"%.2f".format(bill.totalAmount)}"

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.binding.tvDate.text = sdf.format(Date(bill.date))

        // list of other participants (excluding payer)
        val others = bill.participants.filter { it.id != bill.paidBy.id }.map { it.name }
        holder.binding.tvParticipants.text = if (others.isEmpty()) {
            "No one else"
        } else {
            "Split with: ${others.joinToString(", ")}"
        }
    }

    override fun getItemCount() = bills.size
}
