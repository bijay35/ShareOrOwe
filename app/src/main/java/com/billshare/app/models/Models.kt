package com.billshare.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Person(
    val id: String = UUID.randomUUID().toString(),
    val name: String
) : Parcelable

@Parcelize
data class SplitBill(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val paidBy: Person,
    val totalAmount: Double,
    val participants: List<Person>,
    val date: Long = System.currentTimeMillis(),
    var isSettled: Boolean = false
) : Parcelable {
    val sharePerPerson: Double get() = totalAmount / participants.size
    val owedByOthers: Map<String, Double> get() =
        participants.filter { it.id != paidBy.id }
            .associate { it.id to sharePerPerson }
}

@Parcelize
data class IOU(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val paidBy: Person,
    val owedTo: Person,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    var isSettled: Boolean = false
) : Parcelable

@Parcelize
data class Settlement(
    val id: String = UUID.randomUUID().toString(),
    val billId: String, // SplitBill ID
    val personId: String, // Person who settled
    val settledAmount: Double, // Amount this person settled
    val date: Long = System.currentTimeMillis()
) : Parcelable

data class Balance(
    val person: Person,
    val netAmount: Double  // positive = is owed money, negative = owes money
)
