package com.billshare.app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.billshare.app.databinding.FragmentSettingsBinding
import com.billshare.app.models.Person
import com.billshare.app.ui.LoginActivity
import com.billshare.app.utils.DataManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.Toast

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadProfile()

        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnClearSplits.setOnClickListener { confirmClear("split bills") { clearSplits() } }
        binding.btnClearIous.setOnClickListener { confirmClear("IOUs") { clearIOUs() } }
        binding.btnDeleteAccount.setOnClickListener { confirmDeleteAccount() }
    }

    private fun loadProfile() {
        val current = DataManager.getCurrentUser(requireContext())
        binding.etProfileName.setText(current?.name ?: "")
    }

    private fun saveProfile() {
        val name = binding.etProfileName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        val current = DataManager.getCurrentUser(requireContext())
        if (current != null) {
            val updated = current.copy(name = name)
            DataManager.saveCurrentUser(requireContext(), updated)
            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmClear(what: String, action: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete all $what?")
            .setMessage("This will permanently remove all $what. Continue?")
            .setPositiveButton("Yes") { _, _ -> action() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearSplits() {
        DataManager.saveSplitBills(requireContext(), emptyList())
        Toast.makeText(requireContext(), "All split bills deleted", Toast.LENGTH_SHORT).show()
    }

    private fun clearIOUs() {
        DataManager.saveIOUs(requireContext(), emptyList())
        Toast.makeText(requireContext(), "All IOUs deleted", Toast.LENGTH_SHORT).show()
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete account?")
            .setMessage("This will erase your profile and all records. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> performDeleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteAccount() {
        DataManager.deleteAccount(requireContext())
        // go back to login
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
