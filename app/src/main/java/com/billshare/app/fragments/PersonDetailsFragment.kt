package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.billshare.app.adapters.PersonBillAdapter
import com.billshare.app.databinding.FragmentPersonDetailsBinding
import com.billshare.app.models.SplitBill
import com.billshare.app.utils.DataManager

class PersonDetailsFragment : Fragment() {

    private var _binding: FragmentPersonDetailsBinding? = null
    private val binding get() = _binding!!

    private var personId: String? = null
    private var bills = listOf<SplitBill>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        personId = arguments?.getString("personId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPersonDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerBills.layoutManager = LinearLayoutManager(requireContext())
        loadBills()
    }

    private fun loadBills() {
        val context = requireContext()
        val allBills = DataManager.getSplitBills(context)
        bills = allBills.filter { it.paidBy.id == personId }
        if (bills.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerBills.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerBills.visibility = View.VISIBLE
            binding.recyclerBills.adapter = PersonBillAdapter(bills)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
