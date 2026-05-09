package com.billshare.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.billshare.app.utils.DataManager
import com.google.android.material.color.DynamicColors

class BillShareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(DataManager.getNightMode(this))
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
