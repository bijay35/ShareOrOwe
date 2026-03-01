package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.billshare.app.adapters.IOUAdapter
import com.billshare.app.adapters.SplitSummaryAdapter
import com.billshare.app.databinding.FragmentHomeBinding
import com.billshare.app.models.IOU
import com.billshare.app.models.SplitBill
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager
import com.google.android.material.tabs.TabLayout
import com.billshare.app.R
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // setup tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Split"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Owe"))

        binding.recyclerSplit.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerIouSummary.layoutManager = LinearLayoutManager(requireContext())

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showSplitTab()
                    1 -> showOweTab()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        // default to first tab
        binding.tabLayout.getTabAt(0)?.select()
    }

    override fun onResume() {
        super.onResume()
        // refresh visible tab
        if (binding.tabLayout.selectedTabPosition == 0) showSplitTab() else showOweTab()
    }

    private fun showSplitTab() {
        binding.layoutSplit.visibility = View.VISIBLE
        binding.layoutIou.visibility = View.GONE
        loadSplitBills()
    }

    private fun showOweTab() {
        binding.layoutSplit.visibility = View.GONE
        binding.layoutIou.visibility = View.VISIBLE
        loadIOUSummary()
    }

    private fun loadSplitBills() {
        val bills = DataManager.getSplitBills(requireContext())
        val pending = bills.filter { !it.isSettled }
        if (pending.isEmpty()) {
            binding.tvEmptySplit.visibility = View.VISIBLE
            binding.recyclerSplit.visibility = View.GONE
        } else {
            binding.tvEmptySplit.visibility = View.GONE
            binding.recyclerSplit.visibility = View.VISIBLE
            binding.recyclerSplit.adapter = SplitSummaryAdapter(pending, onSettle = { bill ->
                // mark settled
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

    private fun loadIOUSummary() {
        val ious = DataManager.getIOUs(requireContext())
        if (ious.isEmpty()) {
            binding.tvEmptyIou.visibility = View.VISIBLE
            binding.recyclerIouSummary.visibility = View.GONE
        } else {
            binding.tvEmptyIou.visibility = View.GONE
            binding.recyclerIouSummary.visibility = View.VISIBLE
            binding.recyclerIouSummary.adapter = IOUAdapter(ious, { iou ->
                val all = DataManager.getIOUs(requireContext())
                val idx = all.indexOfFirst { it.id == iou.id }
                if (idx >= 0) {
                    all[idx] = all[idx].copy(isSettled = true)
                    DataManager.saveIOUs(requireContext(), all)
                    loadIOUSummary()
                }
            }, onItemClick = { iou ->
                val bundle = Bundle().apply { putString("iouId", iou.id) }
                findNavController().navigate(R.id.iouDetailsFragment, bundle)
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
