package com.example.trzezwadroga.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val mood: String,
    val hungerLevel: Int, // 1-10
    val note: String,
    val relapseSignals: String,
    val gratitude: String = "",
    val triggers: String = "",
    val tomorrowGoal: String = ""
)
