package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.billshare.app.databinding.FragmentIouBinding
import com.billshare.app.models.IOU
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IOUFragment : Fragment() {

    private var _binding: FragmentIouBinding? = null
    private val binding get() = _binding!!
    private var persons = mutableListOf<Person>()
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var owesDirection: Boolean? = null // true = I owe them, false = they owe me

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadPersons()
        setupDatePicker()
        binding.btnAddIOU.setOnClickListener { saveIOU() }
        binding.radioGroupDirection.setOnCheckedChangeListener { _, checkedId ->
            owesDirection = when (checkedId) {
                binding.radioIOwe.id -> true
                binding.radioTheyOwe.id -> false
                else -> null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPersons()
    }

    private fun loadPersons() {
        val all = DataManager.getPersons(requireContext())
        val current = DataManager.getCurrentUser(requireContext())
        persons = if (current != null) all.filter { it.id != current.id }.toMutableList() else all.toMutableList()
        if (persons.isEmpty()) {
            Toast.makeText(requireContext(), "Need at least one other person to record an IOU", Toast.LENGTH_LONG).show()
            return
        }
        val names = persons.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPerson.adapter = adapter
        binding.spinnerPerson.setSelection(0)
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

        val other = persons[binding.spinnerPerson.selectedItemPosition]
        val current = DataManager.getCurrentUser(requireContext())

        if (owesDirection == null) {
            Toast.makeText(requireContext(), "Select who owes whom", Toast.LENGTH_SHORT).show()
            return
        }
        if (current == null) {
            Toast.makeText(requireContext(), "No current user set", Toast.LENGTH_SHORT).show()
            return
        }

        val (paidBy, owedTo) = if (owesDirection == true) {
            Pair(current, other)
        } else {
            Pair(other, current)
        }

        val iou = IOU(
            description = description,
            paidBy = paidBy,
            owedTo = owedTo,
            amount = amount,
            date = selectedDateMillis
        )

        val ious = DataManager.getIOUs(requireContext())
        ious.add(iou)
        DataManager.saveIOUs(requireContext(), ious)
        Toast.makeText(requireContext(), "${paidBy.name} owes ${owedTo.name} $" + "%.2f".format(amount), Toast.LENGTH_LONG).show()
        binding.etDescription.text?.clear()
        binding.etAmount.text?.clear()
        selectedDateMillis = System.currentTimeMillis()
        binding.etDate.setText(formatDate(selectedDateMillis))
        binding.spinnerPerson.setSelection(0)
        binding.radioGroupDirection.clearCheck()
        owesDirection = null
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
