package com.example.trzezwadroga

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.trzezwadroga.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Twoja Droga", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        userProfile?.let {
            val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.sobrietyStartDate)
            Text("$days dni trzeźwości", style = MaterialTheme.typography.displayMedium)

            Spacer(modifier = Modifier.height(16.dp))

            val healthTip = when {
                days >= 365 -> "Rok wolności! Twoje ryzyko chorób serca spadło o połowę."
                days >= 100 -> "100 dni! Twoja wątroba zregenerowała się znacząco."
                days >= 14 -> "2 tygodnie: Twoja cera staje się zdrowsza, a sen głębszy."
                days >= 2 -> "2 dni: Twoje ciśnienie krwi wraca do normy."
                else -> "Każda godzina to zwycięstwo. Organizm zaczyna usuwać toksyny."
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Text(
                    text = healthTip,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } ?: Button(onClick = { viewModel.updateProfile(System.currentTimeMillis()) }) {
            Text("Rozpocznij Licznik")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { /* Navigate to SOS */ },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("SOS - Potrzebuję wsparcia")
        }
    }
}

@Composable
fun AchievementsScreen(viewModel: MainViewModel) {
    val achievements by viewModel.achievements.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Pokój Trofeów", style = MaterialTheme.typography.headlineMedium) }
        items(achievements) { achievement ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (achievement.isUnlocked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (achievement.isUnlocked) "🏅" else "🔒", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(achievement.title, style = MaterialTheme.typography.titleMedium)
                        Text(achievement.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun JournalScreen() {
    var mood by remember { mutableStateOf("") }
    var hungerScale by remember { mutableStateOf(5f) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Dzienniczek", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = mood,
                onValueChange = { mood = it },
                label = { Text("Jak się dziś czujesz?") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Poziom głodu: ${hungerScale.toInt()}")
            Slider(
                value = hungerScale,
                onValueChange = { hungerScale = it },
                valueRange = 1f..10f,
                steps = 8
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Sygnały ostrzegawcze (zaznacz jeśli wystąpiły):", style = MaterialTheme.typography.titleMedium)
        }

        val signals = listOf(
            "Napięcie i frustracja", "Problemy ze snem", "Nawrót do starych znajomości",
            "Unikanie spotkań terapeutycznych", "Przekonanie o wyleczeniu", "Huśtawki nastrojów"
        )

        items(signals) { signal ->
            var checked by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { checked = it })
                Text(signal)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Save */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Zapisz wpis")
            }
        }
    }
}

@Composable
fun HaltScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Autodiagnoza HALT", style = MaterialTheme.typography.headlineMedium)
        Text("Sprawdź swoje podstawowe potrzeby", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        HaltItem("Hungry (Głodny)", "Czy jadłeś coś pożywnego w ciągu ostatnich 4 godzin?")
        HaltItem("Angry (Zły)", "Czy czujesz narastającą irytację lub gniew?")
        HaltItem("Lonely (Samotny)", "Czy czujesz potrzebę rozmowy z kimś bliskim?")
        HaltItem("Tired (Zmęczony)", "Czy Twój organizm potrzebuje odpoczynku lub snu?")

        Spacer(modifier = Modifier.height(32.dp))
        Text("Jeśli na którekolwiek pytanie odpowiedziałeś TAK, zatrzymaj się i zadbaj o tę potrzebę przed podjęciem jakiejkolwiek ważnej decyzji.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { /* SOS Call */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("SOS - Kontakt z opiekunem")
        }
    }
}

@Composable
fun HaltItem(title: String, question: String) {
    var checked by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(question, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = { checked = it })
        }
    }
}
