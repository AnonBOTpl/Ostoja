package com.example.trzezwadroga

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.trzezwadroga.data.entity.HungerDataPoint
import com.example.trzezwadroga.data.entity.JournalEntry
import com.example.trzezwadroga.ui.theme.SageGreen
import com.example.trzezwadroga.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
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

            LaunchedEffect(days) {
                viewModel.checkSobrietyAchievements(days)
            }

            Text("$days dni trzeźwości", style = MaterialTheme.typography.displayMedium)

            val savings = days * it.dailyExpense
            Text("Zaoszczędzone: ${"%.2f".format(savings)} PLN", color = SageGreen, style = MaterialTheme.typography.titleLarge)

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
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:112")
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("SOS - Potrzebuję wsparcia")
        }
    }
}

@Composable
fun HungerChart(data: List<HungerDataPoint>) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Brak danych do wykresu", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val avgHunger = data.map { it.maxHunger }.average().toFloat()
    val minDate = data.first().dayDate
    val maxDate = System.currentTimeMillis()
    val totalDays = maxOf(1L, (maxDate - minDate) / 86400000 + 1)

    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 24f
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(top = 16.dp, bottom = 32.dp, start = 32.dp, end = 16.dp)
    ) {
        val width = size.width
        val height = size.height
        val xFactor = width / maxOf(1f, totalDays.toFloat() - 1)
        val yFactor = height / 10f

        // Grid lines and Y labels
        listOf(1, 5, 10).forEach { label ->
            val y = height - (label * yFactor)
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(label.toString(), -30f, y + 10f, labelPaint)
        }

        // Average line
        val avgY = height - (avgHunger * yFactor)
        drawLine(
            color = Color.Red.copy(alpha = 0.4f),
            start = androidx.compose.ui.geometry.Offset(0f, avgY),
            end = androidx.compose.ui.geometry.Offset(width, avgY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
        drawContext.canvas.nativeCanvas.drawText("Średnia", width - 80f, avgY - 10f, labelPaint)

        // Main Trend Path (Bezier)
        if (data.size > 1) {
            val mainPath = Path()
            val fillPath = Path()

            data.forEachIndexed { index, point ->
                val relativeDay = (point.dayDate - minDate) / 86400000
                val x = relativeDay * xFactor
                val y = height - (point.maxHunger * yFactor)

                if (index == 0) {
                    mainPath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevPoint = data[index - 1]
                    val prevRelativeDay = (prevPoint.dayDate - minDate) / 86400000
                    val prevX = prevRelativeDay * xFactor
                    val prevY = height - (prevPoint.maxHunger * yFactor)

                    mainPath.cubicTo(
                        prevX + (x - prevX) / 2, prevY,
                        prevX + (x - prevX) / 2, y,
                        x, y
                    )
                    fillPath.cubicTo(
                        prevX + (x - prevX) / 2, prevY,
                        prevX + (x - prevX) / 2, y,
                        x, y
                    )
                }

                if (index == data.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(SageGreen.copy(alpha = 0.3f), Color.Transparent)
                )
            )
            drawPath(
                path = mainPath,
                color = SageGreen,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Today highlight
        val lastPoint = data.last()
        val lastX = ((lastPoint.dayDate - minDate) / 86400000) * xFactor
        val lastY = height - (lastPoint.maxHunger * yFactor)
        drawCircle(color = SageGreen, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(lastX, lastY))
        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(lastX, lastY))
    }
}

@Composable
fun ChartsScreen(viewModel: MainViewModel) {
    val trendData by viewModel.hungerTrend.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Analityka Postępów", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Poziom Głodu (Max/Dzień)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                HungerChart(data = trendData)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Analiza długofalowa pozwala dostrzec realny spadek siły nałogu wraz z upływem czasu.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun JournalListScreen(viewModel: MainViewModel, onAddNew: () -> Unit) {
    val entries by viewModel.journalEntries.collectAsState()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = "Nowy wpis")
            }
        }
    ) { padding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            item { Text("Twój Dziennik", style = MaterialTheme.typography.headlineMedium) }
            items(entries) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dateFormat.format(Date(entry.date)), style = MaterialTheme.typography.labelSmall)
                            Text("Głód: ${entry.hungerLevel}/10", color = SageGreen, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(entry.mood, style = MaterialTheme.typography.titleMedium)
                        if (entry.gratitude.isNotEmpty()) {
                            Text("Wdzięczność: ${entry.gratitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        if (entry.relapseSignals.isNotEmpty()) {
                            Text("Sygnały: ${entry.relapseSignals}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (entries.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Brak wpisów. Zacznij pisać już dziś!")
                    }
                }
            }
        }
    }
}

@Composable
fun AddJournalEntryScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val scrollState = rememberScrollState()
    var mood by remember { mutableStateOf("") }
    var hungerScale by remember { mutableStateOf(5f) }
    var gratitude by remember { mutableStateOf("") }
    var triggers by remember { mutableStateOf("") }
    var tomorrowGoal by remember { mutableStateOf("") }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Nowy Wpis", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = mood, onValueChange = { mood = it }, label = { Text("Jak się dziś czujesz?") }, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = gratitude, onValueChange = { gratitude = it }, label = { Text("Za co jesteś dziś wdzięczny?") }, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = triggers, onValueChange = { triggers = it }, label = { Text("Co Cię dziś kusiło? (Triggery)") }, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = tomorrowGoal, onValueChange = { tomorrowGoal = it }, label = { Text("Twój cel na jutro") }, modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))

        Spacer(modifier = Modifier.height(24.dp))
        Text("Poziom głodu: ${hungerScale.toInt()}", style = MaterialTheme.typography.titleMedium)
        Slider(value = hungerScale, onValueChange = { hungerScale = it }, valueRange = 1f..10f, steps = 8)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Sygnały ostrzegawcze:", style = MaterialTheme.typography.titleMedium)

        allSignals.forEach { signal ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Checkbox(checked = selectedSignals[signal] ?: false, onCheckedChange = { selectedSignals[signal] = it })
                Text(signal, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val entry = JournalEntry(
                    mood = mood,
                    hungerLevel = hungerScale.toInt(),
                    note = "",
                    relapseSignals = selectedSignals.filter { it.value }.keys.joinToString(", "),
                    gratitude = gratitude,
                    triggers = triggers,
                    tomorrowGoal = tomorrowGoal
                )
                viewModel.addJournalEntry(entry)
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text("Zapisz wpis")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text("Anuluj")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AchievementsScreen(viewModel: MainViewModel) {
    val achievements by viewModel.achievements.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Pokój Trofeów", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(achievements) { achievement ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (achievement.isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
