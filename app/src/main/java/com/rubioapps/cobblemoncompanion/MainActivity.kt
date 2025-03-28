package com.rubioapps.cobblemoncompanion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log // Mantenemos Log para errores de permisos
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // Necesario para el ViewModel de la Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel // Para ViewModels en Composables de NavHost
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.rubioapps.cobblemoncompanion.ui.ChatbotScreen
import com.rubioapps.cobblemoncompanion.ui.PokemonDetailScreen
import com.rubioapps.cobblemoncompanion.ui.SettingsScreen
import com.rubioapps.cobblemoncompanion.ui.pokedex.PokedexScreen
import com.rubioapps.cobblemoncompanion.ui.theme.CobblemonCompanionTheme
import com.rubioapps.cobblemoncompanion.viewmodel.PokedexViewModel
import dagger.hilt.android.AndroidEntryPoint

// Data class para los ítems de la barra de navegación (movida fuera de MainActivity)
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Mantenemos el ViewModel aquí porque el launcher lo necesita para llamar a processUserJson
    private val pokedexViewModel: PokedexViewModel by viewModels()

    // Launcher para permisos
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permiso concedido, lanza el selector de archivos
                pickJsonLauncher.launch(createJsonIntent())
            } else {
                // Permiso denegado, podrías mostrar un Toast o Snackbar si quieres
                Log.w("MainActivity", "Permiso de lectura denegado por el usuario.")
            }
        }

    // Launcher para seleccionar archivo JSON
    private val pickJsonLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    // Llama al ViewModel de la Activity para procesar
                    pokedexViewModel.processUserJson(uri)
                    // Podrías añadir un mensaje de "JSON importado" aquí (Snackbar)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Pasamos la función lambda que encapsula la lógica de importación
            CobblemonCompanionApp(onRequestImport = ::requestImport)
        }
    }

    // Inicia el proceso de importación (verifica/pide permiso, luego lanza picker)
    private fun requestImport() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when (ContextCompat.checkSelfPermission(this, permission)) {
            PackageManager.PERMISSION_GRANTED -> {
                pickJsonLauncher.launch(createJsonIntent())
            }
            else -> {
                // Solicita permiso
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    // Crea el Intent para el selector de archivos JSON
    private fun createJsonIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json" // Solo muestra archivos JSON
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    // RequestPermissionComposable ya no es necesario aquí, la lógica está en requestImport
}

// Composable principal de la App con Navegación y Scaffold
@Composable
fun CobblemonCompanionApp(
    onRequestImport: () -> Unit // Recibe la función lambda para importar
) {
    val navController = rememberNavController()
    // Obtiene la instancia única del ViewModel asociada al NavHost
    val pokedexViewModel: PokedexViewModel = hiltViewModel()

    val bottomNavItems = listOf(
        BottomNavItem("Pokedex", Icons.AutoMirrored.Filled.List, AppDestinations.POKEDEX_ROUTE),
        BottomNavItem("ChatBot", Icons.Filled.AccountCircle, AppDestinations.CHATBOT_ROUTE),
        BottomNavItem("Settings", Icons.Filled.Settings, AppDestinations.SETTINGS_ROUTE)
    )

    CobblemonCompanionTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestinations.POKEDEX_ROUTE,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppDestinations.POKEDEX_ROUTE) {
                    // Pasa la instancia del ViewModel y la lambda de clic
                    PokedexScreen(
                        uiState = pokedexViewModel.pokedexState.value,
                        viewModel = pokedexViewModel,
                        onPokemonClick = { pokemonName ->
                            navController.navigate(AppDestinations.pokemonDetailRoute(pokemonName))
                        }
                    )
                }
                composable(AppDestinations.CHATBOT_ROUTE) {
                    ChatbotScreen() // Obtendrá su propio ViewModel más adelante
                }
                composable(AppDestinations.SETTINGS_ROUTE) {
                    // Pasa la función lambda para importar
                    SettingsScreen(onRequestImport = onRequestImport)
                }
                composable(
                    route = AppDestinations.POKEMON_DETAIL_ROUTE,
                    arguments = listOf(navArgument("pokemonName") { type = NavType.StringType })
                ) {
                    // PokemonDetailScreen obtiene su ViewModel con hiltViewModel() internamente
                    PokemonDetailScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}