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
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SplitBillFragment : Fragment() {

    private var _binding: FragmentSplitBillBinding? = null
    private val binding get() = _binding!!
    private var persons = mutableListOf<Person>()
    private val selectedParticipants = mutableListOf<Person>()

    // user-selected date for the bill (millis)
    private var selectedDateMillis: Long = System.currentTimeMillis()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSplitBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPersons()
        setupDatePicker()

        binding.btnAddSplitBill.setOnClickListener { saveSplitBill() }
    }

    override fun onResume() {
        super.onResume()
        // refresh list when returning from other screens
        loadPersons()
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
        // default selection to current user if present
        DataManager.getCurrentUser(requireContext())?.let { current ->
            val idx = persons.indexOfFirst { it.id == current.id }
            if (idx >= 0) binding.spinnerPaidBy.setSelection(idx)
        }

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
        // ensure date is shown if user hasn't touched field
        binding.etDate.setText(formatDate(selectedDateMillis))
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
            participants = selectedParticipants.toList(),
            date = selectedDateMillis
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
        // reset date back to today
        selectedDateMillis = System.currentTimeMillis()
        binding.etDate.setText(formatDate(selectedDateMillis))
        loadPersons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupDatePicker() {
        binding.etDate.setText(formatDate(selectedDateMillis))
        binding.etDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                selectedDateMillis = cal.timeInMillis
                binding.etDate.setText(formatDate(selectedDateMillis))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(millis)
    }
}
