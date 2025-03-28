package com.rubioapps.cobblemoncompanion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button // Importa el botón de Compose
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    // Pasamos la función para solicitar permiso e importar
    onRequestImport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Configuración")
        Spacer(modifier = Modifier.height(16.dp))
        // Aquí movemos el botón de importar
        Button(onClick = onRequestImport) {
            Text("Importar JSON de Cobblemon")
        }
        // Puedes añadir más opciones de configuración aquí
    }
}

// Necesitaremos pasar la lógica de RequestPermissionComposable a donde se llame a SettingsScreen
// o rediseñarla un poco. Por ahora, definimos la pantalla.