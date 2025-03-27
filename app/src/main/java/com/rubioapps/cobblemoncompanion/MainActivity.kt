package com.rubioapps.cobblemoncompanion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rubioapps.cobblemoncompanion.ui.pokedex.PokedexScreen
import com.rubioapps.cobblemoncompanion.ui.theme.CobblemonCompanionTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels // Para obtener el ViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.rubioapps.cobblemoncompanion.ui.ChatbotScreen
import com.rubioapps.cobblemoncompanion.ui.SettingsScreen
import com.rubioapps.cobblemoncompanion.viewmodel.PokedexViewModel

// Data class para los ítems de la barra de navegación
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Inyectar Gson ya no es necesario aquí si el ViewModel lo maneja
    // @Inject
    // lateinit var gson: Gson

    // Obtener instancia del ViewModel (Hilt se encarga de crearlo y proveer dependencias)
    private val pokedexViewModel: PokedexViewModel by viewModels()

    // Launcher para permisos y selección de archivo (lo necesitaremos en Settings)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pickJsonLauncher.launch(createJsonIntent())
            } else {
                Log.w("Permisos", "Permiso de lectura denegado.")
            }
        }

    // El launcher para seleccionar el archivo AHORA llama al ViewModel
    private val pickJsonLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                uri?.let {
                    //Antiguo
                    //loadJsonData(it)

                    //nuevo
                    // Llamamos al ViewModel para procesar el URI
                    pokedexViewModel.processUserJson(it)
                }
            }
        }

    // Ya no necesitamos este estado aquí, el ViewModel lo gestionará
    // private var cobblemonDataState = mutableStateOf<CobblemonData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CobblemonCompanionApp(pokedexViewModel = pokedexViewModel,
                onRequestImport = { requestImport() }) // Pasamos la función de importación
            /*CobblemonCompanionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        //Antiguo
                        //RequestPermissionScreen()
                        //Spacer(modifier = Modifier.height(16.dp))

                        // Pasamos los datos a PokedexScreen
                        //PokedexScreen(cobblemonData = cobblemonDataState.value)

                        //nuevo (ya no. el botón lo hace ahora el RequestPermissionComposable)
                        // El botón ahora solo lanza el picker, el resultado lo maneja el launcher
                        //Button(onClick = { openFilePicker() }) { // Simplificado
                        //    Text("Importar JSON de Cobblemon")
                        //}

                        //Spacer(modifier = Modifier.height(16.dp))

                        // Pasamos el ESTADO del ViewModel a PokedexScreen
                        PokedexScreen(uiState = pokedexViewModel.pokedexState.value)
                    }
                }
            }
            RequestPermissionComposable() // Un Composable para manejar el permiso y el botón
            */
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickJsonLauncher.launch(intent)
    }

    // Composable mejorado para el botón y permiso
    @Composable
    fun RequestPermissionComposable() {
        val context = androidx.compose.ui.platform.LocalContext.current
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                openFilePicker()
            } else {
                // Mostrar mensaje de permiso denegado si quieres
                Log.w("Permisos", "Permiso de lectura denegado.")
            }
        }

        Button(onClick = {
            if (hasPermission) {
                openFilePicker()
            } else {
                permissionLauncher.launch(permission)
            }
        }) {
            Text("Importar JSON de Cobblemon")
        }
    }

    // Función para iniciar el proceso de importación (verifica permisos)
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
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    // Función auxiliar para crear el Intent del selector de JSON
    private fun createJsonIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }
}

    @OptIn(ExperimentalMaterial3Api::class) // Necesario para Scaffold
    @Composable
    fun CobblemonCompanionApp(
        pokedexViewModel: PokedexViewModel = hiltViewModel(), // Obtenemos ViewModel con Hilt aquí
        onRequestImport: () -> Unit // Recibimos la función para importar
    ) {
        val navController = rememberNavController()
        // Lista de items para la barra inferior
        val bottomNavItems = listOf(
            BottomNavItem("Pokedex", Icons.Filled.List, AppDestinations.POKEDEX_ROUTE),
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
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        // on the back stack as users select items
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination when
                                        // reselecting the same item
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
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
                    startDestination = AppDestinations.POKEDEX_ROUTE, // Empieza en la Pokedex
                    modifier = Modifier.padding(innerPadding) // Aplica el padding del Scaffold
                ) {
                    composable(AppDestinations.POKEDEX_ROUTE) {
                        // Pasamos el estado del ViewModel a la pantalla Pokedex
                        PokedexScreen(uiState = pokedexViewModel.pokedexState.value)
                    }
                    composable(AppDestinations.CHATBOT_ROUTE) {
                        ChatbotScreen()
                    }
                    composable(AppDestinations.SETTINGS_ROUTE) {
                        // Pasamos la función de importación a la pantalla de Settings
                        SettingsScreen(onRequestImport = onRequestImport)
                    }
                    // Aquí añadiríamos la navegación a la pantalla de detalle más adelante
                    // composable("pokemon_detail/{pokemonName}") { backStackEntry -> ... }
                }
            }
        }
    }