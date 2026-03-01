package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.billshare.app.adapters.IOUAdapter
import com.billshare.app.databinding.FragmentIouBinding
import com.billshare.app.models.IOU
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager

class IOUFragment : Fragment() {

    private var _binding: FragmentIouBinding? = null
    private val binding get() = _binding!!
    private var persons = mutableListOf<Person>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerIOUs.layoutManager = LinearLayoutManager(requireContext())
        loadPersons()
        binding.btnAddIOU.setOnClickListener { saveIOU() }
    }

    override fun onResume() {
        super.onResume()
        loadPersons()
        loadIOUs()
    }

    private fun loadPersons() {
        persons = DataManager.getPersons(requireContext())
        val names = persons.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPaidBy.adapter = adapter
        binding.spinnerOwedTo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun loadIOUs() {
        val ious = DataManager.getIOUs(requireContext())
        if (ious.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerIOUs.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerIOUs.visibility = View.VISIBLE
            binding.recyclerIOUs.adapter = IOUAdapter(ious) { iou ->
                // Settle IOU
                val all = DataManager.getIOUs(requireContext())
                val idx = all.indexOfFirst { it.id == iou.id }
                if (idx >= 0) {
                    all[idx] = all[idx].copy(isSettled = true)
                    DataManager.saveIOUs(requireContext(), all)
                    loadIOUs()
                    Toast.makeText(requireContext(), "Marked as settled!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveIOU() {
        if (persons.isEmpty()) {
            Toast.makeText(requireContext(), "Please add people in the People tab first!", Toast.LENGTH_LONG).show()
            return
        }

        val description = binding.etDescription.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()

        if (description.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val paidBy = persons[binding.spinnerPaidBy.selectedItemPosition]
        val owedTo = persons[binding.spinnerOwedTo.selectedItemPosition]

        if (paidBy.id == owedTo.id) {
            Toast.makeText(requireContext(), "Payer and receiver can't be the same person!", Toast.LENGTH_SHORT).show()
            return
        }

        val iou = IOU(
            description = description,
            paidBy = paidBy,
            owedTo = owedTo,
            amount = amount
        )

        val ious = DataManager.getIOUs(requireContext())
        ious.add(iou)
        DataManager.saveIOUs(requireContext(), ious)
        Toast.makeText(requireContext(), "${paidBy.name} owes ${owedTo.name} $" + "%.2f".format(amount), Toast.LENGTH_LONG).show()
        binding.etDescription.text?.clear()
        binding.etAmount.text?.clear()
        loadIOUs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
