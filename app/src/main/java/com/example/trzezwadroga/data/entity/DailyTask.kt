package com.example.trzezwadroga.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_tasks")
data class DailyTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean? = null, // null = pending, true = done, false = failed
    val timestamp: Long = System.currentTimeMillis()
)
