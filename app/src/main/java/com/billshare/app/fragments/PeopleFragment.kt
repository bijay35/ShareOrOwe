package com.billshare.app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.billshare.app.adapters.PersonAdapter
import com.billshare.app.databinding.FragmentPeopleBinding
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager
import com.billshare.app.utils.SwipeToDeleteCallback
import com.google.android.material.snackbar.Snackbar

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

        adapter = PersonAdapter(persons, { person ->
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
                        refreshEmptyState()
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }, { person ->
            // share a simple text report for this person
            val report = DataManager.getFormattedReportForUser(requireContext(), person)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Transactions for ${person.name}")
                putExtra(Intent.EXTRA_TEXT, report)
            }
            startActivity(Intent.createChooser(intent, "Share report"))
        })

        binding.recyclerPeople.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPeople.adapter = adapter

        val swipe = SwipeToDeleteCallback(
            onSwiped = { pos ->
                val person = persons.getOrNull(pos) ?: return@SwipeToDeleteCallback
                if (hasUnsettled(person)) {
                    adapter.notifyItemChanged(pos)
                    Snackbar.make(binding.root, "Cannot delete: ${person.name} has unsettled bills/IOUs", Snackbar.LENGTH_LONG).show()
                } else {
                    deletePersonWithUndo(person, pos)
                }
            },
            canSwipe = { pos ->
                val person = persons.getOrNull(pos) ?: return@SwipeToDeleteCallback false
                val current = DataManager.getCurrentUser(requireContext())
                current == null || current.id != person.id
            }
        )
        ItemTouchHelper(swipe).attachToRecyclerView(binding.recyclerPeople)

        binding.emptyPeople.ivEmptyIcon.setImageResource(android.R.drawable.ic_menu_myplaces)
        binding.emptyPeople.tvEmptyTitle.text = "No people added"
        binding.emptyPeople.tvEmptySubtitle.text = "Add a name above to start tracking shared expenses"

        persons.clear()
        var all = DataManager.getPersons(requireContext())
        val current = DataManager.getCurrentUser(requireContext())

        if (current != null) {
            val userExists = all.any { it.id == current.id }
            if (!userExists) {
                all = all.toMutableList()
                all.add(current)
                DataManager.savePersons(requireContext(), all)
            }
            persons.addAll(all.filter { it.id != current.id })
        } else {
            persons.addAll(all)
        }
        adapter.notifyDataSetChanged()
        refreshEmptyState()

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
            refreshEmptyState()
            binding.etPersonName.text?.clear()
            Toast.makeText(requireContext(), "$name added!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deletePersonWithUndo(person: Person, position: Int) {
        val ctx = requireContext()
        val snapshot = DataManager.getPersons(ctx).toList()
        persons.removeAt(position)
        adapter.notifyItemRemoved(position)
        DataManager.savePersons(ctx, persons)
        refreshEmptyState()

        Snackbar.make(binding.root, "Removed ${person.name}", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                DataManager.savePersons(ctx, snapshot)
                persons.add(position.coerceAtMost(persons.size), person)
                adapter.notifyItemInserted(position.coerceAtMost(persons.size - 1))
                refreshEmptyState()
            }
            .show()
    }

    private fun refreshEmptyState() {
        if (persons.isEmpty()) {
            binding.emptyPeople.root.visibility = View.VISIBLE
            binding.recyclerPeople.visibility = View.GONE
        } else {
            binding.emptyPeople.root.visibility = View.GONE
            binding.recyclerPeople.visibility = View.VISIBLE
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
