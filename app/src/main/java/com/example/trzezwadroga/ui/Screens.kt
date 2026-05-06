package com.example.trzezwadroga

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.trzezwadroga.data.entity.HungerDataPoint
import com.example.trzezwadroga.data.entity.JournalEntry
import com.example.trzezwadroga.ui.theme.SageGreen
import com.example.trzezwadroga.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val trendData by viewModel.hungerTrend.collectAsState()

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

            LaunchedEffect(days) {
                viewModel.checkSobrietyAchievements(days)
            }

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

            Spacer(modifier = Modifier.height(24.dp))
            Text("Trend Poziomu Głodu", style = MaterialTheme.typography.titleMedium)
            HungerChart(data = trendData)

        } ?: Button(onClick = { viewModel.updateProfile(System.currentTimeMillis()) }) {
            Text("Rozpocznij Licznik")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:112")
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("SOS - Potrzebuję wsparcia")
        }
    }
}

@Composable
fun HungerChart(data: List<HungerDataPoint>) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Brak danych do wykresu", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val minDate = data.first().dayDate
    val maxDate = System.currentTimeMillis()
    val totalDays = maxOf(1L, (maxDate - minDate) / 86400000)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(vertical = 16.dp, horizontal = 24.dp)
    ) {
        val width = size.width
        val height = size.height

        val xFactor = width / totalDays.toFloat()
        val yFactor = height / 10f

        val path = Path()
        data.forEachIndexed { index, point ->
            val relativeDay = (point.dayDate - minDate) / 86400000
            val x = relativeDay * xFactor
            val y = height - (point.maxHunger * yFactor)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = SageGreen,
            style = Stroke(width = 3.dp.toPx())
        )

        // Highlight today's potential point or the last recorded one
        val lastPoint = data.last()
        val lastX = ((lastPoint.dayDate - minDate) / 86400000) * xFactor
        val lastY = height - (lastPoint.maxHunger * yFactor)

        drawCircle(
            color = SageGreen,
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY)
        )
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
fun JournalScreen(viewModel: MainViewModel) {
    var mood by remember { mutableStateOf("") }
    var hungerScale by remember { mutableStateOf(5f) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allSignals = listOf(
        "Napięcie i frustracja", "Problemy ze snem", "Nawrót do starych znajomości",
        "Unikanie spotkań terapeutycznych", "Przekonanie o wyleczeniu", "Huśtawki nastrojów",
        "Brak dbałości o higienę/wygląd", "Zmiana nawyków żywieniowych", "Izolacja społeczna",
        "Szukanie winy w innych", "Defetyzm i użalanie się", "Nierealne plany",
        "Marzenia o piciu kontrolowanym", "Agresywne zachowania", "Nuda i apatia",
        "Pomijanie posiłków", "Zaniedbywanie hobby", "Kłamstwa i sekrety",
        "Euforia bez powodu", "Wracanie do miejsc picia", "Odstawienie leków/witamin"
    )

    val selectedSignals = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
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

            items(allSignals) { signal ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedSignals[signal] ?: false,
                        onCheckedChange = { selectedSignals[signal] = it }
                    )
                    Text(signal)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val entry = JournalEntry(
                            date = System.currentTimeMillis(),
                            mood = mood,
                            hungerLevel = hungerScale.toInt(),
                            note = "",
                            relapseSignals = selectedSignals.filter { it.value }.keys.joinToString(", ")
                        )
                        viewModel.addJournalEntry(entry)

                        mood = ""
                        hungerScale = 5f
                        selectedSignals.clear()

                        scope.launch {
                            snackbarHostState.showSnackbar("Wpis został zapisany")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zapisz wpis")
                }
            }
        }
    }
}

@Composable
fun HaltScreen(viewModel: MainViewModel) {
    var showBreathing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showBreathing) {
        BreathingDialog(
            onDismiss = { showBreathing = false },
            onComplete = {
                viewModel.onBreathingExerciseCompleted()
                showBreathing = false
            }
        )
    }

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

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showBreathing = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Technika Oddechowa (Kryzys)")
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                viewModel.onHaltTestCompleted()
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:112")
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("SOS - Kontakt z opiekunem")
        }
    }
}

@Composable
fun BreathingDialog(onDismiss: () -> Unit, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf("Wdech...") }
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(scale) {
        phase = if (scale > 1.1f) "Wydech..." else "Wdech..."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uspokój Oddech") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(phase)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Podążaj za kręgiem przez 30 sekund.")
            }
        },
        confirmButton = {
            Button(onClick = onComplete) { Text("Zakończone") }
        }
    )
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
