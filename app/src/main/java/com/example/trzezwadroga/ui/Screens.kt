package com.example.trzezwadroga

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun SetupWizardScreen(viewModel: MainViewModel, onComplete: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }
    var sobrietyTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var dailyExpense by remember { mutableStateOf("30") }

    fun showDateTimePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = sobrietyTimestamp }
        DatePickerDialog(context, { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                sobrietyTimestamp = calendar.timeInMillis
                step = 2
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

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
                Button(onClick = { showDateTimePicker() }) { Text("Wybierz Datę i Godzinę") }
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
                Text("Nie pijemy tylko dzisiaj. Skup się na najbliższych 24 godzinach.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = {
                    viewModel.updateProfile(sobrietyTimestamp, dailyExpense.toDoubleOrNull() ?: 30.0)
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
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(48.dp))
            Text("Twoja Droga", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "Ustawienia") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        userProfile?.let { profile ->
            val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - profile.sobrietyStartDate)
            LaunchedEffect(days) { viewModel.checkSobrietyAchievements(days) }

            Text("$days dni trzeźwości", style = MaterialTheme.typography.displayMedium)
            val savings = days * profile.dailyExpense
            Text("Zaoszczędzone: ${"%.2f".format(savings)} PLN", color = SageGreen, style = MaterialTheme.typography.titleLarge)

            if (profile.motivationText.isNotEmpty() || profile.motivationImageUri.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Moje Dlaczego", style = MaterialTheme.typography.titleMedium)
                        if (profile.motivationImageUri.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(profile.motivationImageUri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (profile.motivationText.isNotEmpty()) {
                            Text(profile.motivationText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            val healthTip = when {
                days >= 365 -> "Rok wolności! Ryzyko chorób serca spadło o połowę."
                days >= 100 -> "100 dni! Wątroba zregenerowała się znacząco."
                days >= 14 -> "2 tygodnie: Cera staje się zdrowsza, a sen głębszy."
                else -> "Każda godzina to zwycięstwo. Organizm zaczyna usuwać toksyny."
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Text(text = healthTip, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val number = userProfile?.sosPhoneNumber ?: "112"
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text("SOS - Potrzebuję wsparcia") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    var expense by remember { mutableStateOf(profile?.dailyExpense?.toString() ?: "30") }
    var motivation by remember { mutableStateOf(profile?.motivationText ?: "") }
    var showResetDialog by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.updateMotivation(context, motivation, it.toString()) }
    }

    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        uri?.let { viewModel.updateSosNumber(context, it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            contactLauncher.launch(null)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Ustawienia", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Bank Motywacji", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = motivation, onValueChange = { motivation = it }, label = { Text("Twoje Dlaczego (tekst)") }, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { viewModel.updateMotivation(context, motivation, profile?.motivationImageUri) }) { Text("Zapisz tekst") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { imageLauncher.launch("image/*") }) { Text("Wybierz zdjęcie") }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Konfiguracja SOS", style = MaterialTheme.typography.titleMedium)
        Text("Aktualny numer: ${profile?.sosPhoneNumber}", style = MaterialTheme.typography.bodySmall)
        Button(onClick = {
            val permission = android.Manifest.permission.READ_CONTACTS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                contactLauncher.launch(null)
            } else {
                permissionLauncher.launch(permission)
            }
        }) { Text("Ustaw numer z kontaktów") }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Finanse", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = expense, onValueChange = { expense = it }, label = { Text("Dzienny koszt nałogu") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { profile?.let { viewModel.updateProfile(it.sobrietyStartDate, expense.toDoubleOrNull() ?: 30.0) } }) { Text("Zapisz koszt") }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Oś czasu zdrowia", style = MaterialTheme.typography.titleMedium)
        listOf("2 dni: Ciśnienie wraca do normy", "14 dni: Poprawa cery i snu", "1 rok: Ryzyko chorób serca -50%").forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("RESETUJ WSZYSTKO") }
        TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) { Text("Powrót") }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Resetuj dane") },
            text = { Text("Wszystkie postępy zostaną skasowane.") },
            confirmButton = { Button(onClick = { viewModel.resetAllData(); showResetDialog = false; onNavigateBack() }) { Text("Usuń") } },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
fun Plan24hScreen(viewModel: MainViewModel) {
    var showBreathing by remember { mutableStateOf(false) }
    var showHalt by remember { mutableStateOf(false) }
    val tasks by viewModel.dailyTasks.collectAsState()
    var newTaskTitle by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (showBreathing) {
        BreathingDialog(onDismiss = { showBreathing = false }, onComplete = { viewModel.onBreathingExerciseCompleted(); showBreathing = false })
    }
    if (showHalt) {
        HaltDialog(onDismiss = { showHalt = false }, onComplete = { viewModel.onHaltCompleted(); showHalt = false })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Plan 24h", style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newTaskTitle, onValueChange = { newTaskTitle = it }, modifier = Modifier.weight(1f), label = { Text("Zadanie") })
            IconButton(onClick = { if (newTaskTitle.isNotBlank()) { viewModel.addTask(newTaskTitle); newTaskTitle = "" } }) { Icon(Icons.Default.Add, contentDescription = null) }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(tasks) { task ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(task.title, modifier = Modifier.weight(1f), style = if (task.isCompleted == true) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { viewModel.toggleTask(task, true) }) { Icon(Icons.Default.Check, contentDescription = null, tint = if (task.isCompleted == true) Color.Green else Color.Gray) }
                        IconButton(onClick = { viewModel.toggleTask(task, false) }) { Icon(Icons.Default.Close, contentDescription = null, tint = if (task.isCompleted == false) Color.Red else Color.Gray) }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { showHalt = true }, modifier = Modifier.weight(1f)) { Text("Test HALT") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showBreathing = true }, modifier = Modifier.weight(1f)) { Text("Oddech") }
        }
        Button(onClick = {
            val number = viewModel.userProfile.value?.sosPhoneNumber ?: "112"
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("SOS") }
        TextButton(onClick = { viewModel.resetDailyPlan() }, modifier = Modifier.fillMaxWidth()) { Text("Wyczyść plan") }
    }
}

@Composable
fun HungerChart(data: List<HungerDataPoint>, onPointClick: (HungerDataPoint) -> Unit) {
    if (data.isEmpty()) { Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Brak danych", style = MaterialTheme.typography.bodySmall) }; return }
    val avgHunger = data.map { it.maxHunger }.average().toFloat()
    val minDate = data.first().dayDate
    val totalDays = maxOf(1L, (System.currentTimeMillis() - minDate) / 86400000 + 1)
    val labelPaint = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 24f }
    Canvas(modifier = Modifier.fillMaxWidth().height(250.dp).padding(top = 16.dp, bottom = 32.dp, start = 32.dp, end = 16.dp).pointerInput(data) {
        detectTapGestures { offset ->
            val xFactor = size.width / maxOf(1f, totalDays.toFloat() - 1)
            data.forEach { if (kotlin.math.abs(offset.x - ((it.dayDate - minDate) / 86400000) * xFactor) < 40f) onPointClick(it) }
        }
    }) {
        val xFactor = size.width / maxOf(1f, totalDays.toFloat() - 1)
        val yFactor = size.height / 10f
        listOf(1, 5, 10).forEach {
            val y = size.height - (it * yFactor)
            drawLine(Color.Gray.copy(alpha = 0.2f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y))
            drawContext.canvas.nativeCanvas.drawText(it.toString(), -30f, y + 10f, labelPaint)
        }
        val avgY = size.height - (avgHunger * yFactor)
        drawLine(Color.Red.copy(alpha = 0.4f), androidx.compose.ui.geometry.Offset(0f, avgY), androidx.compose.ui.geometry.Offset(size.width, avgY), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        if (data.size > 1) {
            val mainPath = Path(); val fillPath = Path()
            data.forEachIndexed { i, p ->
                val x = ((p.dayDate - minDate) / 86400000) * xFactor; val y = size.height - (p.maxHunger * yFactor)
                if (i == 0) { mainPath.moveTo(x, y); fillPath.moveTo(x, size.height); fillPath.lineTo(x, y) }
                else {
                    val prev = data[i-1]; val px = ((prev.dayDate - minDate)/86400000)*xFactor; val py = size.height - (prev.maxHunger*yFactor)
                    mainPath.cubicTo(px+(x-px)/2, py, px+(x-px)/2, y, x, y); fillPath.cubicTo(px+(x-px)/2, py, px+(x-px)/2, y, x, y)
                }
                if (i == data.size-1) { fillPath.lineTo(x, size.height); fillPath.close() }
            }
            drawPath(fillPath, Brush.verticalGradient(listOf(SageGreen.copy(alpha=0.3f), Color.Transparent)))
            drawPath(mainPath, SageGreen, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        }
        val last = data.last(); val lx = ((last.dayDate - minDate)/86400000)*xFactor; val ly = size.height - (last.maxHunger*yFactor)
        drawCircle(SageGreen, 6.dp.toPx(), androidx.compose.ui.geometry.Offset(lx, ly))
        drawCircle(Color.White, 3.dp.toPx(), androidx.compose.ui.geometry.Offset(lx, ly))
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
            shape = RoundedCornerShape(24.dp),
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
    val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = "Nowy wpis")
            }
        }
    ) { padding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            item { Text("Twój Dziennik", style = MaterialTheme.typography.headlineMedium) }
            items(entries) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(df.format(Date(entry.date)), style = MaterialTheme.typography.labelSmall)
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

        OutlinedTextField(value = mood, onValueChange = { mood = it }, label = { Text("Jak się dziś czujesz?") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = gratitude, onValueChange = { gratitude = it }, label = { Text("Za co jesteś dziś wdzięczny?") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = triggers, onValueChange = { triggers = it }, label = { Text("Co Cię dziś kusiło? (Triggery)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = tomorrowGoal, onValueChange = { tomorrowGoal = it }, label = { Text("Twój cel na jutro") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

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
            shape = RoundedCornerShape(12.dp)
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
    val achs by viewModel.achievements.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Pokój Trofeów", style = MaterialTheme.typography.headlineMedium); Spacer(modifier = Modifier.height(16.dp)) }
        items(achs) { a -> Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (a.isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (a.isUnlocked) "🏅" else "🔒", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.width(16.dp))
                Column { Text(a.title, style = MaterialTheme.typography.titleMedium); Text(a.description, style = MaterialTheme.typography.bodySmall) }
            }
        } }
    }
}

@Composable
fun BreathingDialog(onDismiss: () -> Unit, onComplete: () -> Unit) {
    var phase by remember { mutableStateOf("Wdech...") }
    val trans = rememberInfiniteTransition(label = ""); val scale by trans.animateFloat(0.8f, 1.5f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "")
    LaunchedEffect(scale) { phase = if (scale > 1.1f) "Wydech..." else "Wdech..." }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Oddech") }, text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(100.dp).scale(scale).background(MaterialTheme.colorScheme.primary.copy(0.3f), CircleShape), contentAlignment = Alignment.Center) { Text(phase) }
        }
    }, confirmButton = { Button(onClick = onComplete) { Text("OK") } })
}

@Composable
fun HaltDialog(onDismiss: () -> Unit, onComplete: () -> Unit) {
    var h by remember { mutableStateOf(false) }
    var a by remember { mutableStateOf(false) }
    var l by remember { mutableStateOf(false) }
    var t by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Autodiagnoza HALT") },
        text = {
            Column {
                Text("Zatrzymaj się i sprawdź, czy nie jesteś:")
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(h, { h = it }); Text("Głodny/a (Hungry)") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(a, { a = it }); Text("Zły/a (Angry)") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(l, { l = it }); Text("Samotny/a (Lonely)") }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(t, { t = it }); Text("Zmęczony/a (Tired)") }
                if (h || a || l || t) {
                    Text("Zadbaj o te potrzeby, zanim podejmiesz decyzję o sięgnięciu po używkę.", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                }
            }
        },
        confirmButton = { Button(onClick = onComplete) { Text("Rozumiem") } }
    )
}
