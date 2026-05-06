package com.example.trzezwadroga

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.trzezwadroga.data.database.AppDatabase
import com.example.trzezwadroga.repository.AppRepository
import androidx.core.content.ContextCompat
import com.example.trzezwadroga.ui.theme.TrzeźwaDrogaTheme
import com.example.trzezwadroga.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {
    private lateinit var viewModel: MainViewModel
    private var isUnlocked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "trzezwa-droga-db"
        ).fallbackToDestructiveMigration().build()
        val repository = AppRepository(db.achievementDao(), db.journalDao(), db.userProfileDao())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        })[MainViewModel::class.java]

        viewModel.initDefaultAchievements()

        showBiometricPrompt()

        setContent {
            TrzeźwaDrogaTheme {
                val unlocked by isUnlocked
                if (unlocked) {
                    MainScreen(viewModel)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aplikacja Zablokowana")
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        finish()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked.value = true
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Logowanie Biometryczne")
            .setSubtitle("Zaloguj się, aby uzyskać dostęp do aplikacji")
            .setNegativeButtonText("Anuluj")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Triple("home", "Home", "🏠"),
                    Triple("journal_list", "Dziennik", "📔"),
                    Triple("charts", "Wykresy", "📊"),
                    Triple("achievements", "Nagrody", "🏆"),
                    Triple("halt", "HALT", "🆘")
                )
                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(icon, fontSize = 20.sp) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                softWrap = false
                            )
                        },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(innerPadding)) {
            composable("home") { HomeScreen(viewModel) }
            composable("journal_list") {
                JournalListScreen(viewModel, onAddNew = { navController.navigate("journal_add") })
            }
            composable("journal_add") {
                AddJournalEntryScreen(viewModel, onNavigateBack = { navController.popBackStack() })
            }
            composable("charts") { ChartsScreen(viewModel) }
            composable("achievements") { AchievementsScreen(viewModel) }
            composable("halt") { HaltScreen(viewModel) }
        }
    }
}
