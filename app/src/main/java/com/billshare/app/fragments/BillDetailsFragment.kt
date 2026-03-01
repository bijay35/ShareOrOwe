package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.billshare.app.databinding.FragmentBillDetailsBinding
import com.billshare.app.models.SplitBill
import com.billshare.app.utils.DataManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillDetailsFragment : Fragment() {
    private var _binding: FragmentBillDetailsBinding? = null
    private val binding get() = _binding!!

    private var billId: String? = null
    private var bill: SplitBill? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billId = arguments?.getString("billId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBillDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadBill()
    }

    private fun loadBill() {
        val context = requireContext()
        val bills = DataManager.getSplitBills(context)
        bill = bills.find { it.id == billId }
        if (bill == null) {
            binding.tvDescription.text = "Not found"
            return
        }
        binding.tvDescription.text = bill!!.description
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvDate.text = sdf.format(Date(bill!!.date))
        binding.tvTotal.text = "\$${"%.2f".format(bill!!.totalAmount)}"
        binding.tvParticipantsCount.text = "Shared with ${bill!!.participants.size} people"
        val current = DataManager.getCurrentUser(context)
        if (current != null) {
            when {
                current.id == bill!!.paidBy.id -> {
                    val oweCount = bill!!.participants.count { it.id != current.id }
                    binding.tvRole.text = "You paid"
                    binding.tvDetail.text = "${oweCount} people owe you each \$${"%.2f".format(bill!!.sharePerPerson)}"
                }
                bill!!.participants.any { it.id == current.id } -> {
                    binding.tvRole.text = "You owe"
                    binding.tvDetail.text = "You owe ${bill!!.paidBy.name} \$${"%.2f".format(bill!!.sharePerPerson)}"
                }
                else -> {
                    binding.tvRole.text = "Not involved"
                    binding.tvDetail.text = ""
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
