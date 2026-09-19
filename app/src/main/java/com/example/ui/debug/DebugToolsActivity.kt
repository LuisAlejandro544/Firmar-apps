package com.example.ui.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MainActivity
import com.example.ui.settings.ThemePreferences
import com.example.ui.settings.ThemeViewModel
import com.example.ui.theme.MyApplicationTheme

/**
 * Actividad independiente de depuración y auditoría (Crypto Lab).
 * Posee su propio launcher icon en el sistema Android y proporciona herramientas avanzadas
 * para auditar certificados ASN.1, verificar firmas matemáticas en vivo y medir el rendimiento
 * del procesador móvil en arquitecturas de 32 y 64 bits.
 */
class DebugToolsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeSettings by themeViewModel.themeSettings.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeMode = themeSettings.themeMode,
                dynamicColor = themeSettings.useDynamicColor,
                customPrimaryColor = themeSettings.customPrimaryColor
            ) {
                DebugToolsContent(
                    onOpenMainActivity = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugToolsContent(
    onOpenMainActivity: () -> Unit,
    forensicViewModel: CertificateForensicViewModel = viewModel(),
    benchmarkViewModel: CryptoBenchmarkViewModel = viewModel()
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabTitles = listOf("Inspector Forense", "Benchmark (32/64-bit)")
    val tabIcons = listOf(Icons.Default.Fingerprint, Icons.Default.Speed)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Crypto Lab (Debug)",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMainActivity) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Ir a Keystore Creator"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMainActivity) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Pantalla Principal"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pestañas superiores para navegar entre las dos herramientas
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) },
                        icon = {
                            Icon(
                                imageVector = tabIcons[index],
                                contentDescription = title
                            )
                        }
                    )
                }
            }

            // Vista de la herramienta seleccionada
            when (selectedTabIndex) {
                0 -> CertificateForensicScreen(viewModel = forensicViewModel)
                1 -> CryptoBenchmarkScreen(viewModel = benchmarkViewModel)
            }
        }
    }
}
