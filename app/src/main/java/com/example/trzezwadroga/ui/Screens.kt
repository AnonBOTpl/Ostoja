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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trzezwadroga.data.entity.DailyTask
import com.example.trzezwadroga.data.entity.HungerDataPoint
import com.example.trzezwadroga.data.entity.JournalEntry
import com.example.trzezwadroga.data.entity.UserProfile
import com.example.trzezwadroga.ui.theme.SageGreen
import com.example.trzezwadroga.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun SetupWizardScreen(viewModel: MainViewModel, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    var dailyExpense by remember { mutableStateOf("30") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (step) {
            1 -> {
                Text("Witaj w Trzeźwej Drodze", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Kiedy zacząłeś swoją przygodę z trzeźwością?", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { step = 2 }) { Text("Dziś (Dalej)") }
            }
            2 -> {
                Text("Finanse i motywacja", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ile średnio wydawałeś dziennie na alkohol?", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = dailyExpense,
                    onValueChange = { dailyExpense = it },
                    label = { Text("Kwota w PLN") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { step = 3 }) { Text("Dalej") }
            }
            3 -> {
                Text("Zasada 24 Godzin", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nie pijemy tylko dzisiaj. Skup się na najbliższych 24 godzinach i swoim planie dnia.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = {
                    viewModel.updateProfile(System.currentTimeMillis(), dailyExpense.toDoubleOrNull() ?: 30.0)
                    onComplete()
                }) { Text("Rozpocznij") }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(48.dp))
            Text("Twoja Droga", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        userProfile?.let {
            val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.sobrietyStartDate)

            LaunchedEffect(days) {
                viewModel.checkSobrietyAchievements(days)
            }

            Text("$days dni trzeźwości", style = MaterialTheme.typography.displayMedium)

            val savings = days * it.dailyExpense
            Text("Zaoszczędzone pieniądze: ${"%.2f".format(savings)} PLN", color = SageGreen, style = MaterialTheme.typography.titleLarge)

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
fun SettingsScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val profile by viewModel.userProfile.collectAsState()
    var expense by remember { mutableStateOf(profile?.dailyExpense?.toString() ?: "30") }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Resetuj wszystko") },
            text = { Text("Czy na pewno chcesz usunąć wszystkie dane? Twoje postępy, wpisy w dzienniku i plan dnia zostaną bezpowrotnie skasowane.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.resetAllData()
                    showResetDialog = false
                    onNavigateBack()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Tak, usuń wszystko")
                }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Anuluj") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Ustawienia", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Dzienny koszt nałogu (PLN)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = expense,
            onValueChange = { expense = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            profile?.let { viewModel.updateProfile(it.sobrietyStartDate, expense.toDoubleOrNull() ?: 30.0) }
        }) { Text("Zapisz zmiany") }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("RESETUJ WSZYSTKO")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text("Powrót")
        }
    }
}

@Composable
fun Plan24hScreen(viewModel: MainViewModel) {
    var showBreathing by remember { mutableStateOf(false) }
    val tasks by viewModel.dailyTasks.collectAsState()
    var newTaskTitle by remember { mutableStateOf("") }
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
        Text("Plan 24h", style = MaterialTheme.typography.headlineMedium)
        Text("Tylko dzisiaj nie pijemy i realizujemy cele.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                modifier = Modifier.weight(1f),
                label = { Text("Dodaj zadanie") }
            )
            IconButton(onClick = {
                if (newTaskTitle.isNotBlank()) {
                    viewModel.addTask(newTaskTitle)
                    newTaskTitle = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(tasks) { task ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, modifier = Modifier.weight(1f),
                            style = if (task.isCompleted == true) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                            else MaterialTheme.typography.bodyLarge)

                        IconButton(onClick = { viewModel.toggleTask(task, true) }) {
                            Icon(Icons.Default.Check, contentDescription = "Wykonane", tint = if (task.isCompleted == true) Color.Green else Color.Gray)
                        }
                        IconButton(onClick = { viewModel.toggleTask(task, false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Niewykonane", tint = if (task.isCompleted == false) Color.Red else Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showBreathing = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Technika Oddechowa (Kryzys)")
        }

        Spacer(modifier = Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = { viewModel.resetDailyPlan() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Wyczyść plan dnia")
        }
    }
}

@Composable
fun HungerChart(
    data: List<HungerDataPoint>,
    onPointClick: (HungerDataPoint) -> Unit
) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Brak danych do wykresu", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val avgHunger = if (data.isNotEmpty()) data.map { it.maxHunger }.average().toFloat() else 0f
    val minDate = if (data.isNotEmpty()) data.first().dayDate else System.currentTimeMillis()
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
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val width = size.width
                    val xFactor = width / maxOf(1f, totalDays.toFloat() - 1)

                    data.forEach { point ->
                        val relativeDay = (point.dayDate - minDate) / 86400000
                        val x = relativeDay * xFactor
                        if (kotlin.math.abs(offset.x - x) < 40f) {
                            onPointClick(point)
                        }
                    }
                }
            }
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
        drawContext.canvas.nativeCanvas.drawText("Średnia", width - 100f, avgY - 10f, labelPaint)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(viewModel: MainViewModel) {
    val trendData by viewModel.hungerTrend.collectAsState()
    val currentTimeRange by viewModel.timeRange.collectAsState()
    var selectedPoint by remember { mutableStateOf<HungerDataPoint?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet && selectedPoint != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                val date = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(selectedPoint!!.dayDate))
                Text(date, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Najwyższy głód: ${selectedPoint!!.maxHunger}/10", color = SageGreen, style = MaterialTheme.typography.titleMedium)
                if (selectedPoint!!.mood.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Zapisany nastrój: ${selectedPoint!!.mood}", style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Analityka Postępów", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        val ranges = listOf("Tydzień", "Miesiąc", "Rok")
        ScrollableTabRow(
            selectedTabIndex = ranges.indexOf(currentTimeRange),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp,
            divider = {}
        ) {
            ranges.forEach { range ->
                Tab(
                    selected = currentTimeRange == range,
                    onClick = { viewModel.setTimeRange(range) },
                    text = { Text(range, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Poziom Głodu (Max)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                HungerChart(data = trendData, onPointClick = {
                    selectedPoint = it
                    showSheet = true
                })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (trendData.isNotEmpty()) {
            val avg = trendData.map { it.maxHunger }.average()
            val today = trendData.last().maxHunger

            if (today < avg) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Dzisiejszy głód jest poniżej Twojej średniej (${"%.1f".format(avg)}). Robisz świetne postępy!",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (trendData.size >= 3) {
                val last3 = trendData.takeLast(3)
                if (last3[2].maxHunger > last3[1].maxHunger && last3[1].maxHunger > last3[0].maxHunger) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Zauważyliśmy wzrost napięcia przez ostatnie 3 dni. Zadbaj o siebie, może warto zadzwonić do kogoś bliskiego?",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFE65100),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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
