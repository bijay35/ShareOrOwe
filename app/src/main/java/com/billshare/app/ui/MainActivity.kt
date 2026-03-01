package com.billshare.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.billshare.app.databinding.ActivityMainBinding
import com.billshare.app.R
import com.billshare.app.utils.DataManager
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ensure logged in
        val current = DataManager.getCurrentUser(this)
        if (current == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        } else {
            supportActionBar?.title = "Hello, ${current.name}"
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
    }
}
