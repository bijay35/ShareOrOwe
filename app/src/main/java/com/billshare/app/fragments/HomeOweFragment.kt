package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.billshare.app.adapters.IOUAdapter
import com.billshare.app.databinding.FragmentHomeOweBinding
import com.billshare.app.models.IOU
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager
import com.billshare.app.utils.SwipeToDeleteCallback
import androidx.navigation.fragment.findNavController
import com.billshare.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomeOweFragment : Fragment() {
    private var _binding: FragmentHomeOweBinding? = null
    private val binding get() = _binding!!

    private var fromTimestamp: Long? = null
    private var toTimestamp: Long? = null

    private enum class SortMode { DATE, AMOUNT, NAME }
    private var sortMode: SortMode = SortMode.DATE
    private var sortAsc: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeOweBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerIouSummary.layoutManager = LinearLayoutManager(requireContext())
        attachSwipeToDelete()
        setupFilters()
        setupDateButtons()
        binding.tvToggleFilters.setOnClickListener {
            val container = binding.filterContainer
            if (container.visibility == View.VISIBLE) {
                container.visibility = View.GONE
                binding.tvToggleFilters.text = "Filter ▼"
            } else {
                container.visibility = View.VISIBLE
                binding.tvToggleFilters.text = "Filter ▲"
            }
        }
        binding.tvResetFilters.setOnClickListener {
            resetFilters()
        }
        setupSort()
    }

    private fun setupSort() {
        binding.chipGroupSort.setOnCheckedStateChangeListener { _, checkedIds ->
            sortMode = when (checkedIds.firstOrNull()) {
                R.id.chip_sort_amount -> SortMode.AMOUNT
                R.id.chip_sort_name -> SortMode.NAME
                else -> SortMode.DATE
            }
            loadIOUSummary()
        }
        binding.btnSortDir.setOnClickListener {
            sortAsc = !sortAsc
            binding.btnSortDir.setIconResource(
                if (sortAsc) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float
            )
            loadIOUSummary()
        }
    }

    private fun applySort(ious: List<IOU>): List<IOU> {
        val sorted = when (sortMode) {
            SortMode.DATE -> ious.sortedBy { it.date }
            SortMode.AMOUNT -> ious.sortedBy { it.amount }
            SortMode.NAME -> ious.sortedBy { it.description.lowercase() }
        }
        return if (sortAsc) sorted else sorted.reversed()
    }

    override fun onResume() {
        super.onResume()
        loadIOUSummary()
    }

    private fun attachSwipeToDelete() {
        val callback = SwipeToDeleteCallback(onSwiped = { pos ->
            val adapter = binding.recyclerIouSummary.adapter as? IOUAdapter ?: return@SwipeToDeleteCallback
            val iou = adapter.iouAt(pos) ?: return@SwipeToDeleteCallback
            deleteIouWithUndo(iou)
        })
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerIouSummary)
    }

    private fun deleteIouWithUndo(iou: IOU) {
        val ctx = requireContext()
        val snapshot = DataManager.getIOUs(ctx).toList()
        val newList = snapshot.filter { it.id != iou.id }.toMutableList()
        DataManager.saveIOUs(ctx, newList)
        loadIOUSummary()

        Snackbar.make(binding.root, "Deleted \"${iou.description}\"", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                DataManager.saveIOUs(ctx, snapshot)
                loadIOUSummary()
            }
            .show()
    }

    private fun loadIOUSummary() {
        var ious: List<IOU> = DataManager.getIOUs(requireContext())

        // limit to IOUs where current user is involved
        val current = DataManager.getCurrentUser(requireContext())
        if (current != null) {
            ious = ious.filter { iou ->
                iou.paidBy.id == current.id || iou.owedTo.id == current.id
            }
        }

        // status filter
        when (binding.spinnerStatus.selectedItem as String) {
            "Settled" -> ious = ious.filter { it.isSettled }
            "Unsettled" -> ious = ious.filter { !it.isSettled }
            // All nothing
        }

        // person filter
        val filterId = binding.spinnerFilter.tag as? String
        if (filterId != null) {
            ious = ious.filter { iou ->
                iou.paidBy.id == filterId || iou.owedTo.id == filterId
            }
        }

        // date range
        fromTimestamp?.let { from -> ious = ious.filter { it.date >= from } }
        toTimestamp?.let { to -> ious = ious.filter { it.date <= to } }

        ious = applySort(ious)

        if (ious.isEmpty()) {
            binding.emptyIou.root.visibility = View.VISIBLE
            binding.emptyIou.ivEmptyIcon.setImageResource(android.R.drawable.ic_menu_send)
            binding.emptyIou.tvEmptyTitle.text = "No IOUs yet"
            binding.emptyIou.tvEmptySubtitle.text = "Record a direct IOU from the Transaction tab"
            binding.recyclerIouSummary.visibility = View.GONE
        } else {
            binding.emptyIou.root.visibility = View.GONE
            binding.recyclerIouSummary.visibility = View.VISIBLE
            binding.recyclerIouSummary.adapter = IOUAdapter(ious, { iou ->
                showSettleDialog(iou, ious)
            }, onItemClick = { iou ->
                val bundle = Bundle().apply { putString("iouId", iou.id) }
                findNavController().navigate(R.id.iouDetailsFragment, bundle)
            })
            
            // Show calculation summary
            val selectedPersonId = binding.spinnerFilter.tag as? String
            val totalText = calculateIOUTotal(ious, selectedPersonId)
            if (totalText != null) {
                binding.tvIouTotal.text = totalText
                binding.tvIouTotal.visibility = View.VISIBLE
            } else {
                binding.tvIouTotal.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFilters() {
        // person spinner
        var allPersons = DataManager.getPersons(requireContext())
        val current = DataManager.getCurrentUser(requireContext())
        
        // Ensure current user is in the people list
        if (current != null) {
            val userExists = allPersons.any { it.id == current.id }
            if (!userExists) {
                allPersons = allPersons.toMutableList()
                allPersons.add(current)
                DataManager.savePersons(requireContext(), allPersons)
            }
        }
        
        // exclude current user from filter options (to show transactions with others)
        val persons = if (current != null) allPersons.filter { it.id != current.id } else allPersons
        val names = mutableListOf("All people")
        names.addAll(persons.map { it.name })
        val ids = mutableListOf<String?>(null)
        ids.addAll(persons.map { it.id })
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = adapter
        binding.spinnerFilter.setSelection(0)
        binding.spinnerFilter.tag = null
        binding.spinnerFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.spinnerFilter.tag = ids.getOrNull(position)
                loadIOUSummary()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // status spinner
        val statuses = listOf("Settled", "Unsettled", "All")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter
        binding.spinnerStatus.setSelection(1) // default to Unsettled
        binding.spinnerStatus.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadIOUSummary()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupDateButtons() {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        binding.btnFromDate.setOnClickListener {
            showDateDialog { y, m, d ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                fromTimestamp = cal.timeInMillis
                binding.btnFromDate.text = fmt.format(cal.time)
                loadIOUSummary()
            }
        }
        binding.btnToDate.setOnClickListener {
            showDateDialog { y, m, d ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(y, m, d, 23, 59, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }
                toTimestamp = cal.timeInMillis
                binding.btnToDate.text = fmt.format(cal.time)
                loadIOUSummary()
            }
        }
    }

    private fun showDateDialog(onSet: (year: Int, month: Int, day: Int) -> Unit) {
        val now = java.util.Calendar.getInstance()
        val dialog = android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
            onSet(y, m, d)
        }, now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH), now.get(java.util.Calendar.DAY_OF_MONTH))
        dialog.show()
    }

    private fun showSettleDialog(iou: IOU, currentFilteredIOUS: List<IOU>) {
        val current = DataManager.getCurrentUser(requireContext()) ?: return
        
        // Get the selected person from dropdown
        val selectedPersonId = binding.spinnerFilter.tag as? String
        val selectedPerson = if (selectedPersonId != null) {
            DataManager.getPersons(requireContext()).find { it.id == selectedPersonId }
        } else null
        
        val options = if (selectedPerson != null) {
            arrayOf("Settle this IOU only", "Settle all IOUs with ${selectedPerson.name}")
        } else {
            arrayOf("Settle this IOU only")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Settle Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> settleSingleIOU(iou)
                    1 -> selectedPerson?.let { settleAllIOUsWithPerson(it) }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun settleSingleIOU(iou: IOU) {
        val all = DataManager.getIOUs(requireContext())
        val idx = all.indexOfFirst { it.id == iou.id }
        if (idx >= 0) {
            all[idx] = all[idx].copy(isSettled = true)
            DataManager.saveIOUs(requireContext(), all)
            loadIOUSummary()
        }
    }
    
    private fun settleAllIOUsWithPerson(person: Person) {
        val current = DataManager.getCurrentUser(requireContext()) ?: return
        val all = DataManager.getIOUs(requireContext())
        
        // Find all unsettled IOUs involving current user and selected person
        val iousToSettle = all.filter { !it.isSettled &&
            ((it.paidBy.id == current.id && it.owedTo.id == person.id) ||
             (it.paidBy.id == person.id && it.owedTo.id == current.id))
        }
        
        if (iousToSettle.isNotEmpty()) {
            val updatedIOUS = all.map { iou ->
                if (iousToSettle.any { it.id == iou.id }) {
                    iou.copy(isSettled = true)
                } else {
                    iou
                }
            }
            DataManager.saveIOUs(requireContext(), updatedIOUS)
            loadIOUSummary()
        }
    }

    private fun calculateIOUTotal(ious: List<IOU>, filterId: String?): String? {
        if (filterId == null) return null
        val current = DataManager.getCurrentUser(requireContext()) ?: return null
        var net = 0.0
        for (iou in ious) {
            if (iou.paidBy.id == current.id && iou.owedTo.id == filterId) {
                net += iou.amount
            } else if (iou.paidBy.id == filterId && iou.owedTo.id == current.id) {
                net -= iou.amount
            }
        }
        val name = binding.spinnerFilter.selectedItem as? String ?: ""
        return when {
            net > 0 -> "You are owed $${"%.2f".format(net)} by $name"
            net < 0 -> "You owe $${"%.2f".format(-net)} to $name"
            else -> null
        }
    }

    private fun resetFilters() {
        binding.spinnerFilter.setSelection(0)
        binding.spinnerFilter.tag = null
        binding.spinnerStatus.setSelection(1)
        fromTimestamp = null
        toTimestamp = null
        binding.btnFromDate.text = "From"
        binding.btnToDate.text = "To"
        loadIOUSummary()
        binding.filterContainer.visibility = View.GONE
        binding.tvToggleFilters.text = "Filter ▼"
    }
}
