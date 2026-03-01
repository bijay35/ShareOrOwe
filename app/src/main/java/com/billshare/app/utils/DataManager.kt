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

    // export/import helpers -------------------------------------------------

    private data class ExportWrapper(
        val persons: List<Person>,
        val splitBills: List<SplitBill>,
        val ious: List<IOU>
    )

    /**
     * Return a JSON string containing everything persisted by the app.
     * This can be saved to a file for backup or shared with another device.
     */
    fun exportAllData(context: Context): String {
        val wrapper = ExportWrapper(
            persons = getPersons(context),
            splitBills = getSplitBills(context),
            ious = getIOUs(context)
        )
        return gson.toJson(wrapper)
    }

    /**
     * Attempt to import the provided JSON string as an entire dataset.  Returns
     * true on success or false if the payload could not be parsed.
     * On success the current preferences are overwritten.
     */
    fun importAllData(context: Context, json: String): Boolean {
        return try {
            val type = object : TypeToken<ExportWrapper>() {}.type
            val wrapper: ExportWrapper = gson.fromJson(json, type)
            // overwrite everything
            savePersons(context, wrapper.persons)
            saveSplitBills(context, wrapper.splitBills)
            saveIOUs(context, wrapper.ious)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Produce a human-readable text report containing all transactions for the
     * requested person.  The output can be shared via plain text.
     */
    fun getFormattedReportForUser(context: Context, person: Person): String {
        val me = getCurrentUser(context)
            ?: return "No current user; cannot generate report."

        val sb = StringBuilder()
        sb.append("Report for ${me.name} and ${person.name}\n")
        sb.append("=======================================\n\n")

        var net = 0.0

        val splits = getSplitBills(context)
        if (splits.isNotEmpty()) {
            sb.append("Split bills:\n")
            for (bill in splits) {
                val involvesPerson = bill.paidBy.id == person.id || bill.participants.any { it.id == person.id }
                val involvesMe = bill.paidBy.id == me.id || bill.participants.any { it.id == me.id }
                if (involvesPerson && involvesMe && !bill.isSettled) {
                    sb.append("- ${bill.description}: total ${bill.totalAmount}, paid by ${bill.paidBy.name}\n")
                    // adjust net: positive means person owes me
                    val share = bill.totalAmount / bill.participants.size
                    when {
                        bill.paidBy.id == me.id && bill.participants.any { it.id == person.id } -> net += share
                        bill.paidBy.id == person.id && bill.participants.any { it.id == me.id } -> net -= share
                        // if paid by third party, ignore for net
                    }
                }
            }
            sb.append("\n")
        }

        val ious = getIOUs(context)
        if (ious.isNotEmpty()) {
            sb.append("Owe :\n")
            for (iou in ious) {
                val involvesPerson = iou.paidBy.id == person.id || iou.owedTo.id == person.id
                val involvesMe = iou.paidBy.id == me.id || iou.owedTo.id == me.id
                if (involvesPerson && involvesMe && !iou.isSettled) {
                    sb.append("- ${iou.description}: ${iou.amount}, paid by ${iou.paidBy.name}\n")
                    if (iou.paidBy.id == me.id && iou.owedTo.id == person.id) net += iou.amount
                    if (iou.paidBy.id == person.id && iou.owedTo.id == me.id) net -= iou.amount
                }
            }
            sb.append("\n")
        }

        // final net summary
        sb.append("Net balance:\n")
        sb.append(
            when {
                net > 0 -> "${person.name} owes ${me.name} ${"%.2f".format(net)}\n"
                net < 0 -> "${me.name} owes ${person.name} ${"%.2f".format(-net)}\n"
                else -> "Even.\n"
            }
        )

        return sb.toString()
    }
}
