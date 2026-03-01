package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.billshare.app.adapters.BalanceAdapter
import com.billshare.app.databinding.FragmentHomeBinding
import com.billshare.app.models.Balance
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerBalances.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        loadBalances()
    }

    private fun loadBalances() {
        val context = requireContext()
        val persons = DataManager.getPersons(context)
        val splitBills = DataManager.getSplitBills(context)
        val ious = DataManager.getIOUs(context)

        // Calculate net balance for each person
        val balanceMap = mutableMapOf<String, Double>()
        persons.forEach { balanceMap[it.id] = 0.0 }

        // From split bills
        for (bill in splitBills) {
            val share = bill.sharePerPerson
            // Payer is owed by everyone else
            balanceMap[bill.paidBy.id] = (balanceMap[bill.paidBy.id] ?: 0.0) + (share * (bill.participants.size - 1))
            // Others owe
            bill.participants.filter { it.id != bill.paidBy.id }.forEach { p ->
                balanceMap[p.id] = (balanceMap[p.id] ?: 0.0) - share
            }
        }

        // From IOUs (not settled)
        for (iou in ious.filter { !it.isSettled }) {
            balanceMap[iou.owedTo.id] = (balanceMap[iou.owedTo.id] ?: 0.0) + iou.amount
            balanceMap[iou.paidBy.id] = (balanceMap[iou.paidBy.id] ?: 0.0) - iou.amount
        }

        val balances = persons.map { person ->
            Balance(person, balanceMap[person.id] ?: 0.0)
        }

        if (balances.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerBalances.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerBalances.visibility = View.VISIBLE
            binding.recyclerBalances.adapter = BalanceAdapter(balances)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
