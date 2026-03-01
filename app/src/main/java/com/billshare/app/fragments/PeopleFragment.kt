package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.billshare.app.adapters.PersonAdapter
import com.billshare.app.databinding.FragmentPeopleBinding
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager

class PeopleFragment : Fragment() {

    private var _binding: FragmentPeopleBinding? = null
    private val binding get() = _binding!!
    private val persons = mutableListOf<Person>()
    private lateinit var adapter: PersonAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPeopleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PersonAdapter(persons) { person ->
            val current = DataManager.getCurrentUser(requireContext())
            if (current != null && current.id == person.id) {
                Toast.makeText(requireContext(), "You cannot remove yourself", Toast.LENGTH_SHORT).show()
                return@PersonAdapter
            }
            // ask for confirmation
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Remove ${person.name}?")
                .setMessage("Are you sure you want to remove this person?")
                .setPositiveButton("Yes") { _, _ ->
                    if (hasUnsettled(person)) {
                        Toast.makeText(requireContext(), "Cannot delete: there are unsettled bills/IOUs", Toast.LENGTH_LONG).show()
                    } else {
                        persons.remove(person)
                        DataManager.savePersons(requireContext(), persons)
                        adapter.notifyDataSetChanged()
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }

        binding.recyclerPeople.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPeople.adapter = adapter

        persons.clear()
        persons.addAll(DataManager.getPersons(requireContext()))
        adapter.notifyDataSetChanged()

        binding.btnAddPerson.setOnClickListener {
            val name = binding.etPersonName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val person = Person(name = name)
            persons.add(person)
            DataManager.savePersons(requireContext(), persons)
            adapter.notifyDataSetChanged()
            binding.etPersonName.text?.clear()
            Toast.makeText(requireContext(), "$name added!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hasUnsettled(person: Person): Boolean {
        val context = requireContext()
        val splitBills = DataManager.getSplitBills(context)
        if (splitBills.any { !it.isSettled && (it.paidBy.id == person.id || it.participants.any { p -> p.id == person.id }) }) {
            return true
        }
        val ious = DataManager.getIOUs(context)
        if (ious.any { !it.isSettled && (it.paidBy.id == person.id || it.owedTo.id == person.id) }) {
            return true
        }
        return false
    }
}
