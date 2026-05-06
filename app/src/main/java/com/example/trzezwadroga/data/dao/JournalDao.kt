package com.example.trzezwadroga.data.dao

import androidx.room.*
import com.example.trzezwadroga.data.entity.HungerDataPoint
import com.example.trzezwadroga.data.entity.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT (date / 86400000) * 86400000 as dayDate, MAX(hungerLevel) as maxHunger FROM journal_entries GROUP BY dayDate ORDER BY dayDate ASC")
    fun getDailyMaxHunger(): Flow<List<HungerDataPoint>>

    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Insert
    suspend fun insertEntry(entry: JournalEntry)
}
