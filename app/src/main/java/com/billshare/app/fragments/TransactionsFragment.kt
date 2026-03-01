package com.billshare.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.billshare.app.databinding.FragmentTransactionsBinding

class TransactionsFragment : Fragment() {
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Split"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Owe"))

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showFragment(SplitBillFragment())
                    1 -> showFragment(IOUFragment())
                }
                // ensure container redraws after switching
                binding.container.requestLayout()
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // delay selecting first tab until layout is ready
        binding.tabLayout.post { binding.tabLayout.getTabAt(0)?.select() }
        // also defer first fragment attachment until container has layout
        binding.container.post { showFragment(SplitBillFragment()) }
    }

    private fun showFragment(fragment: Fragment) {
        // use commitNow to ensure fragment is added immediately and visible
        childFragmentManager.beginTransaction()
            .replace(binding.container.id, fragment)
            .commitNow()
        binding.container.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        // ensure split fragment exists when fragment becomes visible
        if (childFragmentManager.fragments.isEmpty()) {
            binding.container.post { showFragment(SplitBillFragment()) }
        }
    }

    override fun onResume() {
        super.onResume()
        // reselect current tab to refresh fragment (covers layout/timing issues)
        binding.tabLayout.post {
            val pos = binding.tabLayout.selectedTabPosition
            binding.tabLayout.getTabAt(pos)?.select()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
