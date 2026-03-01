package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.billshare.app.databinding.FragmentSplitBillBinding
import com.billshare.app.models.Person
import com.billshare.app.models.SplitBill
import com.billshare.app.utils.DataManager

class SplitBillFragment : Fragment() {

    private var _binding: FragmentSplitBillBinding? = null
    private val binding get() = _binding!!
    private var persons = mutableListOf<Person>()
    private val selectedParticipants = mutableListOf<Person>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSplitBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPersons()

        binding.btnAddSplitBill.setOnClickListener { saveSplitBill() }
    }

    private fun loadPersons() {
        persons = DataManager.getPersons(requireContext())

        if (persons.isEmpty()) {
            Toast.makeText(requireContext(), "Please add people in the People tab first!", Toast.LENGTH_LONG).show()
            return
        }

        val names = persons.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPaidBy.adapter = adapter

        // Dynamically add checkboxes for participants
        binding.checkboxContainer.removeAllViews()
        selectedParticipants.clear()
        persons.forEach { person ->
            val checkBox = CheckBox(requireContext())
            checkBox.text = person.name
            checkBox.isChecked = true
            selectedParticipants.add(person)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedParticipants.add(person)
                else selectedParticipants.remove(person)
            }
            binding.checkboxContainer.addView(checkBox)
        }
    }

    private fun saveSplitBill() {
        val description = binding.etDescription.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()

        if (description.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedParticipants.isEmpty()) {
            Toast.makeText(requireContext(), "Select at least one participant", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val paidBy = persons[binding.spinnerPaidBy.selectedItemPosition]
        val bill = SplitBill(
            description = description,
            paidBy = paidBy,
            totalAmount = amount,
            participants = selectedParticipants.toList()
        )

        val bills = DataManager.getSplitBills(requireContext())
        bills.add(bill)
        DataManager.saveSplitBills(requireContext(), bills)

        Toast.makeText(requireContext(), "Bill saved! Each person owes $" + "%.2f".format(bill.sharePerPerson), Toast.LENGTH_LONG).show()
        clearForm()
    }

    private fun clearForm() {
        binding.etDescription.text?.clear()
        binding.etAmount.text?.clear()
        loadPersons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
