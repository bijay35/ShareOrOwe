package com.billshare.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.billshare.app.databinding.ItemPersonBinding
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager
import android.view.View

class PersonAdapter(
    private val persons: MutableList<Person>,
    private val onDelete: (Person) -> Unit
) : RecyclerView.Adapter<PersonAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPersonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val person = persons[position]
        holder.binding.tvPersonName.text = person.name
        // hide delete button for current user
        val current = DataManager.getCurrentUser(holder.binding.root.context)
        if (current != null && current.id == person.id) {
            holder.binding.btnDelete.visibility = View.GONE
        } else {
            holder.binding.btnDelete.visibility = View.VISIBLE
            holder.binding.btnDelete.setOnClickListener { onDelete(person) }
        }
    }

    override fun getItemCount() = persons.size
}
