package com.billshare.app.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.billshare.app.R
import com.billshare.app.databinding.ActivityMainBinding
import com.billshare.app.utils.DataManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val current = DataManager.getCurrentUser(this)
        if (current == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Hello, ${current.name}"

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_toggle_theme) {
                toggleTheme()
                true
            } else false
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun toggleTheme() {
        val isCurrentlyDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        val newMode = if (isCurrentlyDark) AppCompatDelegate.MODE_NIGHT_NO
                      else AppCompatDelegate.MODE_NIGHT_YES
        DataManager.setNightMode(this, newMode)
        AppCompatDelegate.setDefaultNightMode(newMode)
    }
}
