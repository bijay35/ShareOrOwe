package com.billshare.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.billshare.app.databinding.ActivityLoginBinding
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isNewUserMode = false

    private val importDataLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            handleImportResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialUI()
    }

    private fun setupInitialUI() {
        // Always show both options - new user or import data
        binding.tvInstruction.text = "Choose how you'd like to get started"
        binding.btnNewUser.visibility = View.VISIBLE
        binding.btnImportData.visibility = View.VISIBLE
        binding.tilName.visibility = View.GONE
        binding.btnContinue.visibility = View.GONE

        binding.btnNewUser.setOnClickListener {
            showNewUserUI()
        }

        binding.btnImportData.setOnClickListener {
            startImportProcess()
        }

        binding.btnContinue.setOnClickListener {
            handleContinueClick()
        }
    }

    private fun showNewUserUI() {
        isNewUserMode = true
        
        // Check if there's already a current user (from import)
        val currentUser = DataManager.getCurrentUser(this)
        
        if (currentUser != null) {
            // User already exists from import, show their name for confirmation
            binding.tvInstruction.text = "Confirm your name or change it"
            binding.etName.setText(currentUser.name)
        } else {
            // No existing user, fresh setup
            binding.tvInstruction.text = "Enter your name to get started"
            binding.etName.setText("")
        }
        
        binding.btnNewUser.visibility = View.GONE
        binding.btnImportData.visibility = View.GONE
        binding.tilName.visibility = View.VISIBLE
        binding.btnContinue.visibility = View.VISIBLE
    }

    private fun startImportProcess() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importDataLauncher.launch(intent)
    }

    private fun handleImportResult(data: Intent?) {
        val uri = data?.data ?: return
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text != null && DataManager.importAllData(this, text)) {
                Toast.makeText(this, "Data imported successfully!", Toast.LENGTH_SHORT).show()
                
                // Check if current user was set during import
                val currentUser = DataManager.getCurrentUser(this)
                if (currentUser != null) {
                    // User automatically identified, proceed to main
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // Import succeeded but no user identified, show new user UI
                    showNewUserUI()
                }
            } else {
                Toast.makeText(this, "Import failed: invalid format", Toast.LENGTH_LONG).show()
                // Fall back to new user setup
                showNewUserUI()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to import: ${e.message}", Toast.LENGTH_LONG).show()
            // Fall back to new user setup
            showNewUserUI()
        }
    }

    private fun handleContinueClick() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etName.error = "Enter your name"
            return
        }

        // Check if there's already a current user (from import)
        val currentUser = DataManager.getCurrentUser(this)
        
        if (currentUser != null) {
            // User already set (from import), just proceed to main
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // New user setup
        val persons = DataManager.getPersons(this)
        var person = persons.find { it.name.equals(name, ignoreCase = true) }
        if (person == null) {
            person = Person(name = name)
            persons.add(person)
            DataManager.savePersons(this, persons)
        }
        DataManager.saveCurrentUser(this, person)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
