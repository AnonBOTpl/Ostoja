package com.example.trzezwadroga.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.example.trzezwadroga.data.entity.*
import com.example.trzezwadroga.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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

    val dailyTasks: StateFlow<List<DailyTask>> = repository.allTasks.stateIn(
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

    fun updateProfile(startDate: Long, dailyExpense: Double = 30.0) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.upsertProfile(
                current?.copy(sobrietyStartDate = startDate, dailyExpense = dailyExpense)
                ?: UserProfile(sobrietyStartDate = startDate, dailyExpense = dailyExpense)
            )
        }
    }

    fun updateMotivation(context: Context, text: String, imageUri: String?) {
        viewModelScope.launch {
            val localPath = imageUri?.let { uriString ->
                if (uriString.startsWith("content://")) {
                    saveImageLocally(context, Uri.parse(uriString))
                } else {
                    uriString
                }
            } ?: ""

            userProfile.value?.let {
                repository.upsertProfile(it.copy(motivationText = text, motivationImageUri = localPath))
            }
        }
    }

    fun onHaltCompleted() {
        viewModelScope.launch {
            val currentProfile = userProfile.value ?: return@launch
            val newCount = currentProfile.haltCount + 1
            repository.upsertProfile(currentProfile.copy(haltCount = newCount))

            if (newCount >= 10) {
                achievements.value.find { it.id == "A2" && !it.isUnlocked }?.let {
                    repository.updateAchievement(it.copy(isUnlocked = true))
                }
            }
        }
    }

    private suspend fun saveImageLocally(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, "motivation_image.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun updateSosNumber(context: Context, contactUri: Uri) {
        viewModelScope.launch {
            val number = fetchPhoneNumber(context, contactUri)
            userProfile.value?.let {
                repository.upsertProfile(it.copy(sosPhoneNumber = number))
            }
        }
    }

    private suspend fun fetchPhoneNumber(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        var number = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        if (cursor?.moveToFirst() == true) {
            val idIdx = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
            val hasPhoneIdx = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)

            if (idIdx != -1 && hasPhoneIdx != -1) {
                val id = cursor.getString(idIdx)
                if (cursor.getString(hasPhoneIdx) == "1") {
                    val phones = context.contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                        null,
                        null
                    )
                    if (phones?.moveToFirst() == true) {
                        val numIdx = phones.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (numIdx != -1) {
                            number = phones.getString(numIdx)
                        }
                    }
                    phones?.close()
                }
            }
        }
        cursor?.close()
        number.ifEmpty { "112" }
    }

    fun addTask(title: String) {
        viewModelScope.launch {
            repository.insertTask(DailyTask(title = title))
        }
    }

    fun toggleTask(task: DailyTask, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = isCompleted))
        }
    }

    fun resetDailyPlan() {
        viewModelScope.launch {
            repository.clearTasks()
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            // Usually we would need to wipe the DB. For simplicity:
            repository.upsertProfile(UserProfile(sobrietyStartDate = System.currentTimeMillis()))
            repository.clearTasks()
            // In a real app we'd trigger a full DB clear via a Room callback
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
        if (entries.size >= 10) {
            achievements.value.find { it.id == "A2" && !it.isUnlocked }?.let {
                repository.updateAchievement(it.copy(isUnlocked = true))
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
                Achievement("A2", "Świadomy", "10 wykonanych testów HALT", category = "ACTIVITY"),
                Achievement("A3", "Zwycięzca", "Użycie techniki oddechowej", category = "ACTIVITY")
            )
            repository.initAchievements(defaults)
        }
    }
}
