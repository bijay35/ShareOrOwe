package com.billshare.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.billshare.app.databinding.ActivityLoginBinding
import com.billshare.app.models.Person
import com.billshare.app.utils.DataManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etName.error = "Enter your name"
                return@setOnClickListener
            }
            // ensure person exists
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
}
