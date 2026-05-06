package com.example.trzezwadroga.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.trzezwadroga.data.dao.AchievementDao
import com.example.trzezwadroga.data.dao.JournalDao
import com.example.trzezwadroga.data.dao.UserProfileDao
import com.example.trzezwadroga.data.entity.Achievement
import com.example.trzezwadroga.data.entity.JournalEntry
import com.example.trzezwadroga.data.entity.UserProfile

@Database(entities = [Achievement::class, JournalEntry::class, UserProfile::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun achievementDao(): AchievementDao
    abstract fun journalDao(): JournalDao
    abstract fun userProfileDao(): UserProfileDao
}
