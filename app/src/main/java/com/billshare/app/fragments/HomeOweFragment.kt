package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.billshare.app.adapters.IOUAdapter
import com.billshare.app.databinding.FragmentHomeOweBinding
import com.billshare.app.models.IOU
import com.billshare.app.utils.DataManager
import androidx.navigation.fragment.findNavController
import com.billshare.app.R

class HomeOweFragment : Fragment() {
    private var _binding: FragmentHomeOweBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeOweBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerIouSummary.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        loadIOUSummary()
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
