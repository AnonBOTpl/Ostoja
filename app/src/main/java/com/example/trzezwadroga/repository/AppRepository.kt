package com.example.trzezwadroga.repository

import com.example.trzezwadroga.data.dao.AchievementDao
import com.example.trzezwadroga.data.dao.JournalDao
import com.example.trzezwadroga.data.dao.UserProfileDao
import com.example.trzezwadroga.data.entity.Achievement
import com.example.trzezwadroga.data.entity.JournalEntry
import com.example.trzezwadroga.data.entity.UserProfile
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val achievementDao: AchievementDao,
    private val journalDao: JournalDao,
    private val userProfileDao: UserProfileDao
) {
    val allAchievements: Flow<List<Achievement>> = achievementDao.getAllAchievements()
    val allJournalEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()

    suspend fun insertJournalEntry(entry: JournalEntry) = journalDao.insertEntry(entry)
    suspend fun updateAchievement(achievement: Achievement) = achievementDao.updateAchievement(achievement)
    suspend fun upsertProfile(profile: UserProfile) = userProfileDao.upsertProfile(profile)
    suspend fun initAchievements(achievements: List<Achievement>) = achievementDao.insertAchievements(achievements)
}
