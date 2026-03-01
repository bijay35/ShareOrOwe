package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ViewSwitcher
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.billshare.app.adapters.SplitSummaryAdapter
import com.billshare.app.databinding.FragmentHomeSplitBinding
import com.billshare.app.models.SplitBill
import com.billshare.app.utils.DataManager
import androidx.navigation.fragment.findNavController
import com.billshare.app.R

class HomeSplitFragment : Fragment() {

    private var _binding: FragmentHomeSplitBinding? = null
    private val binding get() = _binding!!

    private var fromTimestamp: Long? = null
    private var toTimestamp: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeSplitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerSplit.layoutManager = LinearLayoutManager(requireContext())
        setupSplitFilter()
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
    }

    override fun onResume() {
        super.onResume()
        loadSplitBills()
    }

    private fun setupSplitFilter() {
        val allPersons = DataManager.getPersons(requireContext())
        val current = DataManager.getCurrentUser(requireContext())
        // exclude current user from dropdown options
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
                val selectedId = ids.getOrNull(position)
                binding.spinnerFilter.tag = selectedId
                loadSplitBills()
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
                loadSplitBills()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun loadSplitBills() {
        // ensure filter section collapsed when loading? leave as is
        val context = requireContext()
        val allBills = DataManager.getSplitBills(context)
        val current = DataManager.getCurrentUser(requireContext())
        // start with all bills (use immutable type for filtering)
        var filtered: List<SplitBill> = allBills

        // status filtering
        when (binding.spinnerStatus.selectedItem as String) {
            "Settled" -> filtered = filtered.filter { it.isSettled }
            "Unsettled" -> filtered = filtered.filter { !it.isSettled }
            // "All" does nothing
        }

        // only consider bills where current user is involved
        if (current != null) {
            filtered = filtered.filter { bill ->
                bill.paidBy.id == current.id || bill.participants.any { it.id == current.id }
            }
        }

        // person filter
        val filterId = binding.spinnerFilter.tag as? String
        filtered = if (filterId == null) {
            filtered
        } else {
            filtered.filter { bill ->
                bill.paidBy.id == filterId || bill.participants.any { it.id == filterId }
            }
        }

        // date range filter
        fromTimestamp?.let { from -> filtered = filtered.filter { it.date >= from } }
        toTimestamp?.let { to -> filtered = filtered.filter { it.date <= to } }

        val totalText = calculateSplitTotal(filtered, filterId)
        if (totalText != null) {
            binding.tvSplitTotal.text = totalText
            binding.tvSplitTotal.visibility = View.VISIBLE
        } else {
            binding.tvSplitTotal.visibility = View.GONE
        }

        if (filtered.isEmpty()) {
            binding.tvEmptySplit.visibility = View.VISIBLE
            binding.recyclerSplit.visibility = View.GONE
        } else {
            binding.tvEmptySplit.visibility = View.GONE
            binding.recyclerSplit.visibility = View.VISIBLE
            binding.recyclerSplit.adapter = SplitSummaryAdapter(filtered, onSettle = { bill ->
                val all = DataManager.getSplitBills(requireContext())
                val idx = all.indexOfFirst { it.id == bill.id }
                if (idx >= 0) {
                    all[idx].isSettled = true
                    DataManager.saveSplitBills(requireContext(), all)
                    loadSplitBills()
                }
            }, onItemClick = { bill ->
                val bundle = Bundle().apply { putString("billId", bill.id) }
                findNavController().navigate(R.id.billDetailsFragment, bundle)
            }, onPayerClick = { payerId ->
                val bundle = Bundle().apply { putString("personId", payerId) }
                findNavController().navigate(R.id.personDetailsFragment, bundle)
            })
        }
    }

    private fun calculateSplitTotal(bills: List<SplitBill>, filterId: String?): String? {
        if (filterId == null) return null
        val current = DataManager.getCurrentUser(requireContext()) ?: return null
        var net = 0.0
        for (bill in bills) {
            val share = bill.sharePerPerson
            if (bill.paidBy.id == current.id && bill.participants.any { it.id == filterId } && filterId != current.id) {
                net += share
            } else if (bill.paidBy.id == filterId && bill.participants.any { it.id == current.id }) {
                net -= share
            }
        }
        val name = binding.spinnerFilter.selectedItem as? String ?: ""
        return when {
            net > 0 -> "You earn $${"%.2f".format(net)} from $name"
            net < 0 -> "You owe $${"%.2f".format(-net)} to $name"
            else -> null
        }
    }

    private fun setupDateButtons() {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        binding.btnFromDate.setOnClickListener {
            showDateDialog { year, month, day ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                fromTimestamp = cal.timeInMillis
                binding.btnFromDate.text = fmt.format(cal.time)
                loadSplitBills()
            }
        }
        binding.btnToDate.setOnClickListener {
            showDateDialog { year, month, day ->
                val cal = java.util.Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }
                toTimestamp = cal.timeInMillis
                binding.btnToDate.text = fmt.format(cal.time)
                loadSplitBills()
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

    private fun resetFilters() {
        binding.spinnerFilter.setSelection(0)
        binding.spinnerFilter.tag = null
        binding.spinnerStatus.setSelection(1)
        fromTimestamp = null
        toTimestamp = null
        binding.btnFromDate.text = "From"
        binding.btnToDate.text = "To"
        loadSplitBills()
        // collapse filters
        binding.filterContainer.visibility = View.GONE
        binding.tvToggleFilters.text = "Filter ▼"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
