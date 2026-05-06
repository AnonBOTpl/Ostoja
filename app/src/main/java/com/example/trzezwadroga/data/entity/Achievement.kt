package com.example.trzezwadroga.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockDate: Long? = null,
    val category: String // "SOBRIETY", "ACTIVITY"
)
