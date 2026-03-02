package com.billshare.app.utils

import android.content.Context
import com.billshare.app.models.IOU
import com.billshare.app.models.Person
import com.billshare.app.models.SplitBill
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.abs

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

        // STEP 1: FILTER RELEVANT BILLS
        val splits = getSplitBills(context)
        val relevantSplits = splits.filter { 
            (it.paidBy.id == me.id || it.paidBy.id == person.id) &&
            (it.participants.any { p -> p.id == me.id } && it.participants.any { p -> p.id == person.id }) &&
            !it.isSettled 
        }

        // Group splits by participant count
        val splitsByPeopleCount = relevantSplits.groupBy { it.participants.size }

        var bijayOwesKamalFromSplits = 0.0
        var kamalOwesBijayFromSplits = 0.0

        // Process each group separately
        splitsByPeopleCount.forEach { (_, splitsInGroup) ->
            val participantNames = splitsInGroup.first().participants.map { it.name }.let { names ->
                if (names.size > 2) names.dropLast(1).joinToString(", ") + " & " + names.last()
                else names.joinToString(" & ")
            }
            sb.append("Split bills: ($participantNames)\n")
            
            var splitCount = 1
            for (bill in splitsInGroup) {
                sb.append("${splitCount++}. ${bill.description}: $${"%.2f".format(bill.totalAmount)} - (Paid by ${bill.paidBy.name})\n")
                
                // STEP 2: CALCULATE SPLIT PORTIONS
                val share = bill.totalAmount / bill.participants.size
                
                // STEP 3: CALCULATE SPLIT BALANCES
                when {
                    // I paid and person participated
                    bill.paidBy.id == me.id && bill.participants.any { it.id == person.id } -> {
                        kamalOwesBijayFromSplits += share
                    }
                    // Person paid and I participated  
                    bill.paidBy.id == person.id && bill.participants.any { it.id == me.id } -> {
                        bijayOwesKamalFromSplits += share
                    }
                    // Multi-person bills (>2 participants) - only consider our portion
                    bill.participants.size > 2 -> {
                        when {
                            bill.paidBy.id == me.id && bill.participants.any { it.id == person.id } -> {
                                kamalOwesBijayFromSplits += share
                            }
                            bill.paidBy.id == person.id && bill.participants.any { it.id == me.id } -> {
                                bijayOwesKamalFromSplits += share
                            }
                        }
                    }
                }
            }
            
            // Show group summary
            val groupBalance = if (bijayOwesKamalFromSplits > kamalOwesBijayFromSplits) {
                bijayOwesKamalFromSplits - kamalOwesBijayFromSplits
            } else {
                kamalOwesBijayFromSplits - bijayOwesKamalFromSplits
            }
            
            if (groupBalance > 0.005) {
                if (bijayOwesKamalFromSplits > kamalOwesBijayFromSplits) {
                    sb.append("# ${me.name} owes ${person.name} $${"%.2f".format(groupBalance)}\n")
                } else {
                    sb.append("# ${person.name} owes ${me.name} $${"%.2f".format(groupBalance)}\n")
                }
            }
            sb.append("\n")
        }

        // STEP 4: ADD DIRECT OWES
        val ious = getIOUs(context)
        val relevantIOUS = ious.filter { 
            ((it.paidBy.id == me.id && it.owedTo.id == person.id) || 
             (it.paidBy.id == person.id && it.owedTo.id == me.id)) &&
            !it.isSettled 
        }
        
        var bijayOwesKamalDirect = 0.0
        var kamalOwesBijayDirect = 0.0

        // Display IOU transactions
        if (relevantIOUS.isNotEmpty()) {
            sb.append("Owe :\n")
            var oweCount = 1
            for (iou in relevantIOUS) {
                sb.append("${oweCount++}. ${iou.description}: $${"%.2f".format(iou.amount)} - (${iou.paidBy.name} owes ${iou.owedTo.name})\n")
                
                // Calculate direct owes
                when {
                    iou.paidBy.id == me.id && iou.owedTo.id == person.id -> bijayOwesKamalDirect += iou.amount
                    iou.paidBy.id == person.id && iou.owedTo.id == me.id -> kamalOwesBijayDirect += iou.amount
                }
            }
            sb.append("\n")
        }

        // STEP 5: CALCULATE NET BALANCE
        val totalBijayOwes = bijayOwesKamalFromSplits + bijayOwesKamalDirect
        val totalKamalOwes = kamalOwesBijayFromSplits + kamalOwesBijayDirect
        val netBalance = totalBijayOwes - totalKamalOwes

        sb.append("Net balance:\n")
        sb.append(
            when {
                netBalance > 0.005 -> "${me.name} owes ${person.name} $${"%.2f".format(netBalance)}\n"
                netBalance < -0.005 -> "${person.name} owes ${me.name} $${"%.2f".format(abs(netBalance))}\n"
                else -> "Balance is settled.\n"
            }
        )

        return sb.toString()
    }
}
