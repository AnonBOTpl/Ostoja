package com.example.trzezwadroga.data.dao

import androidx.room.*
import com.example.trzezwadroga.data.entity.DailyTask
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {
    @Query("SELECT * FROM daily_tasks ORDER BY id ASC")
    fun getAllTasks(): Flow<List<DailyTask>>

    @Insert
    suspend fun insertTask(task: DailyTask)

    @Update
    suspend fun updateTask(task: DailyTask)

    @Delete
    suspend fun deleteTask(task: DailyTask)

    @Query("DELETE FROM daily_tasks")
    suspend fun clearAllTasks()
}
