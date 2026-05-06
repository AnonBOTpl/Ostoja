package com.example.trzezwadroga.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val sobrietyStartDate: Long,
    val name: String = "",
    val dailyExpense: Double = 30.0, // Default cost of addiction per day in PLN
    val motivationText: String = "",
    val motivationImageUri: String = "",
    val sosPhoneNumber: String = "112",
    val haltCount: Int = 0
)
