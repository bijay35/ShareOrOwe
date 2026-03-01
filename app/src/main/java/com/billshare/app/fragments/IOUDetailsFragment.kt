package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.billshare.app.databinding.FragmentIouDetailsBinding
import com.billshare.app.models.IOU
import com.billshare.app.utils.DataManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IOUDetailsFragment : Fragment() {
    private var _binding: FragmentIouDetailsBinding? = null
    private val binding get() = _binding!!

    private var iouId: String? = null
    private var iou: IOU? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iouId = arguments?.getString("iouId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIouDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadIOU()
    }

    private fun loadIOU() {
        val all = DataManager.getIOUs(requireContext())
        iou = all.find { it.id == iouId }
        if (iou == null) {
            binding.tvDescription.text = "Not found"
            return
        }
        binding.tvDescription.text = iou!!.description
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvDate.text = sdf.format(Date(iou!!.date))
        binding.tvAmount.text = "\$${"%.2f".format(iou!!.amount)}"

        val current = DataManager.getCurrentUser(requireContext())
        if (current != null) {
            if (current.id == iou!!.paidBy.id) {
                binding.tvRole.text = "You paid"
                binding.tvDetail.text = "${iou!!.owedTo.name} owes you \$${"%.2f".format(iou!!.amount)}"
            } else if (current.id == iou!!.owedTo.id) {
                binding.tvRole.text = "You owe"
                binding.tvDetail.text = "You owe ${iou!!.paidBy.name} \$${"%.2f".format(iou!!.amount)}"
            } else {
                binding.tvRole.text = "Not involved"
                binding.tvDetail.text = ""
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
