package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.billshare.app.R
import com.billshare.app.databinding.FragmentHomeBinding
import com.billshare.app.utils.DataManager
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment =
                if (position == 0) HomeSplitFragment() else HomeOweFragment()
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = if (pos == 0) "Split" else "Owe"
        }.attach()

        binding.btnSummaryClose.setOnClickListener {
            DataManager.setHomeSummaryVisible(requireContext(), false)
            binding.cardSummary.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        applySummaryVisibility()
        refreshSummary()
    }

    private fun applySummaryVisibility() {
        binding.cardSummary.visibility =
            if (DataManager.isHomeSummaryVisible(requireContext())) View.VISIBLE else View.GONE
    }

    private fun refreshSummary() {
        val ctx = requireContext()
        val me = DataManager.getCurrentUser(ctx)
        if (me == null) {
            binding.tvSummaryNet.text = "Welcome"
            binding.tvSummaryOwed.text = "$0.00"
            binding.tvSummaryOwe.text = "$0.00"
            return
        }

        var owedToMe = 0.0
        var iOwe = 0.0

        for (bill in DataManager.getSplitBills(ctx)) {
            val share = bill.sharePerPerson
            if (bill.paidBy.id == me.id) {
                bill.participants
                    .filter { it.id != me.id }
                    .forEach { p ->
                        if (!DataManager.isPersonSettledForBill(ctx, bill.id, p.id)) {
                            owedToMe += share
                        }
                    }
            } else if (bill.participants.any { it.id == me.id }) {
                if (!DataManager.isPersonSettledForBill(ctx, bill.id, me.id)) {
                    iOwe += share
                }
            }
        }

        for (iou in DataManager.getIOUs(ctx)) {
            if (iou.isSettled) continue
            when {
                iou.paidBy.id == me.id && iou.owedTo.id != me.id -> iOwe += iou.amount
                iou.owedTo.id == me.id && iou.paidBy.id != me.id -> owedToMe += iou.amount
            }
        }

        val net = owedToMe - iOwe
        binding.tvSummaryOwed.text = "$" + "%.2f".format(owedToMe)
        binding.tvSummaryOwe.text = "$" + "%.2f".format(iOwe)
        binding.tvSummaryNet.text = when {
            net > 0.005 -> "You're owed $" + "%.2f".format(net)
            net < -0.005 -> "You owe $" + "%.2f".format(-net)
            else -> "All settled"
        }

        val color = when {
            net > 0.005 -> R.color.amount_positive
            net < -0.005 -> R.color.amount_negative
            else -> R.color.amount_neutral
        }
        binding.tvSummaryNet.setTextColor(ContextCompat.getColor(ctx, color))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
