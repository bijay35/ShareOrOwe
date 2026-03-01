package com.billshare.app.utils

import android.content.Context
import com.billshare.app.models.IOU
import com.billshare.app.models.Person
import com.billshare.app.models.SplitBill
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DataManager {
    private const val PREFS_NAME = "BillSharePrefs"
    private const val KEY_PERSONS = "persons"
    private const val KEY_SPLIT_BILLS = "split_bills"
    private const val KEY_IOUS = "ious"
    private const val KEY_CURRENT_USER = "current_user"
    private val gson = Gson()

    fun savePersons(context: Context, persons: List<Person>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PERSONS, gson.toJson(persons)).apply()
    }

    fun getPersons(context: Context): MutableList<Person> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PERSONS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Person>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveSplitBills(context: Context, bills: List<SplitBill>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SPLIT_BILLS, gson.toJson(bills)).apply()
    }

    fun getSplitBills(context: Context): MutableList<SplitBill> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SPLIT_BILLS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<SplitBill>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveIOUs(context: Context, ious: List<IOU>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_IOUS, gson.toJson(ious)).apply()
    }

    fun getIOUs(context: Context): MutableList<IOU> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_IOUS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<IOU>>() {}.type
        return gson.fromJson(json, type)
    }

    // current user handling
    fun saveCurrentUser(context: Context, person: Person) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_USER, gson.toJson(person)).apply()
    }

    fun getCurrentUser(context: Context): Person? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CURRENT_USER, null) ?: return null
        return gson.fromJson(json, Person::class.java)
    }

    fun clearData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PERSONS).remove(KEY_SPLIT_BILLS).remove(KEY_IOUS).apply()
    }

    fun deleteAccount(context: Context) {
        // clear everything including current user
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
