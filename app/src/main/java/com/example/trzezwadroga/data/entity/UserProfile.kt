package com.example.trzezwadroga.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val sobrietyStartDate: Long,
    val name: String = ""
)
