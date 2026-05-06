package com.example.trzezwadroga.data.dao

import androidx.room.*
import com.example.trzezwadroga.data.entity.JournalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Insert
    suspend fun insertEntry(entry: JournalEntry)
}
