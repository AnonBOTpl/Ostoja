package com.example.trzezwadroga.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trzezwadroga.data.entity.Achievement
import com.example.trzezwadroga.data.entity.HungerDataPoint
import com.example.trzezwadroga.data.entity.JournalEntry
import com.example.trzezwadroga.data.entity.UserProfile
import com.example.trzezwadroga.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val achievements: StateFlow<List<Achievement>> = repository.allAchievements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _timeRange = MutableStateFlow("Tydzień")
    val timeRange: StateFlow<String> = _timeRange

    val hungerTrend: StateFlow<List<HungerDataPoint>> = combine(
        repository.hungerTrend,
        _timeRange
    ) { trend, range ->
        val now = System.currentTimeMillis()
        val filtered = when (range) {
            "Tydzień" -> trend.filter { it.dayDate >= now - 7 * 86400000L }
            "Miesiąc" -> trend.filter { it.dayDate >= now - 30 * 86400000L }
            "Rok" -> trend.filter { it.dayDate >= now - 365 * 86400000L }
            else -> trend
        }

        if (range == "Rok") {
            // Aggregate by month for the year view
            filtered.groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.dayDate }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
            }.map { (_, points) ->
                HungerDataPoint(
                    dayDate = points.first().dayDate,
                    maxHunger = points.map { it.maxHunger }.average().toInt()
                )
            }.sortedBy { it.dayDate }
        } else {
            filtered
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setTimeRange(range: String) {
        _timeRange.value = range
    }

    val journalEntries: StateFlow<List<JournalEntry>> = repository.allJournalEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.insertJournalEntry(entry)
            checkActivityAchievements()
        }
    }

    fun updateProfile(startDate: Long) {
        viewModelScope.launch {
            repository.upsertProfile(UserProfile(sobrietyStartDate = startDate))
        }
    }

    fun checkSobrietyAchievements(days: Long) {
        viewModelScope.launch {
            val achievementsList = achievements.value
            val updates = mutableListOf<Achievement>()

            fun unlock(id: String) {
                achievementsList.find { it.id == id && !it.isUnlocked }?.let {
                    updates.add(it.copy(isUnlocked = true))
                }
            }

            if (days >= 1) unlock("S1")
            if (days >= 3) unlock("S2")
            if (days >= 7) unlock("S3")
            if (days >= 30) unlock("S4")
            if (days >= 100) unlock("S5")
            if (days >= 365) unlock("S6")

            updates.forEach { repository.updateAchievement(it) }
        }
    }

    private suspend fun checkActivityAchievements() {
        val entries = journalEntries.value
        if (entries.size >= 7) {
            achievements.value.find { it.id == "A1" && !it.isUnlocked }?.let {
                repository.updateAchievement(it.copy(isUnlocked = true))
            }
        }
    }

    private var haltTestCount = 0
    fun onHaltTestCompleted() {
        viewModelScope.launch {
            haltTestCount++
            if (haltTestCount >= 10) {
                achievements.value.find { it.id == "A2" && !it.isUnlocked }?.let {
                    repository.updateAchievement(it.copy(isUnlocked = true))
                }
            }
        }
    }

    fun onBreathingExerciseCompleted() {
        viewModelScope.launch {
            achievements.value.find { it.id == "A3" && !it.isUnlocked }?.let {
                repository.updateAchievement(it.copy(isUnlocked = true))
            }
        }
    }

    fun initDefaultAchievements() {
        viewModelScope.launch {
            val defaults = listOf(
                Achievement("S1", "24 godziny", "Pierwszy krok ku wolności", category = "SOBRIETY"),
                Achievement("S2", "3 dni", "Twój organizm zaczyna się oczyszczać", category = "SOBRIETY"),
                Achievement("S3", "1 tydzień", "To już siedem dni zwycięstwa!", category = "SOBRIETY"),
                Achievement("S4", "1 miesiąc", "Miesiąc nowej drogi życia", category = "SOBRIETY"),
                Achievement("S5", "100 dni", "Trzycyfrowe zwycięstwo!", category = "SOBRIETY"),
                Achievement("S6", "1 rok", "Rok pełen świadomych wyborów", category = "SOBRIETY"),
                Achievement("A1", "Dziennikarz", "7 dni regularnych wpisów", category = "ACTIVITY"),
                Achievement("A2", "Świadomy", "Wykonanie 10 testów HALT", category = "ACTIVITY"),
                Achievement("A3", "Zwycięzca", "Użycie techniki oddechowej", category = "ACTIVITY")
            )
            repository.initAchievements(defaults)
        }
    }
}
