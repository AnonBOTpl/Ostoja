package com.example.trzezwadroga.repository

import com.example.trzezwadroga.data.dao.AchievementDao
import com.example.trzezwadroga.data.dao.DailyTaskDao
import com.example.trzezwadroga.data.dao.JournalDao
import com.example.trzezwadroga.data.dao.UserProfileDao
import com.example.trzezwadroga.data.entity.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val achievementDao: AchievementDao,
    private val journalDao: JournalDao,
    private val userProfileDao: UserProfileDao,
    private val dailyTaskDao: DailyTaskDao
) {
    val allAchievements: Flow<List<Achievement>> = achievementDao.getAllAchievements()
    val allJournalEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()
    val hungerTrend: Flow<List<HungerDataPoint>> = journalDao.getDailyMaxHunger()
    val allTasks: Flow<List<DailyTask>> = dailyTaskDao.getAllTasks()

    suspend fun insertJournalEntry(entry: JournalEntry) = journalDao.insertEntry(entry)
    suspend fun updateAchievement(achievement: Achievement) = achievementDao.updateAchievement(achievement)
    suspend fun upsertProfile(profile: UserProfile) = userProfileDao.upsertProfile(profile)
    suspend fun initAchievements(achievements: List<Achievement>) = achievementDao.insertAchievements(achievements)

    suspend fun insertTask(task: DailyTask) = dailyTaskDao.insertTask(task)
    suspend fun updateTask(task: DailyTask) = dailyTaskDao.updateTask(task)
    suspend fun clearTasks() = dailyTaskDao.clearAllTasks()
}
