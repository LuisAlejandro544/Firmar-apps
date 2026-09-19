package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.detail.KeystoreDetailScreen
import com.example.ui.detail.KeystoreDetailViewModel
import com.example.ui.generator.GeneratorScreen
import com.example.ui.generator.GeneratorViewModel
import com.example.ui.list.KeystoreListScreen
import com.example.ui.list.KeystoreListViewModel
import com.example.ui.navigation.AppRoutes
import com.example.ui.navigation.NavDestination
import com.example.ui.settings.ColorPickerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.ThemeViewModel
import com.example.ui.theme.MyApplicationTheme

/**
 * Actividad principal de la aplicación.
 * Configura la experiencia Edge-to-Edge, el tema reactivo global y el contenedor de navegación.
 */
class MainActivity : ComponentActivity() {
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
                KeystoreApp(themeViewModel = themeViewModel)
            }
        }
    }
}

/**
 * Contenedor principal con sistema de navegación cómodo mediante NavigationBar inferior
 * y soporte para pantallas de Generación, Listado, Detalle y Configuración.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeystoreApp(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ocultar la barra inferior y top app bar general en pantallas secundarias/hijas
    val isChildScreen = currentRoute?.startsWith("keystore_detail") == true || currentRoute == AppRoutes.COLOR_THEME

    val destinations = listOf(
        NavDestination.Generator,
        NavDestination.KeystoreList,
        NavDestination.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = if (!isChildScreen) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isChildScreen) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Escudo de seguridad",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !isChildScreen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_bottom_navigation")
                ) {
                    destinations.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isChildScreen) PaddingValues(0.dp) else innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = AppRoutes.GENERATOR,
                modifier = Modifier.fillMaxSize()
            ) {
                // Pantalla 1: Formulario Generador de Keystores
                composable(AppRoutes.GENERATOR) {
                    val generatorViewModel: GeneratorViewModel = viewModel()
                    GeneratorScreen(
                        viewModel = generatorViewModel,
                        onNavigateToDetail = { keystoreId ->
                            navController.navigate(AppRoutes.createDetailRoute(keystoreId))
                        }
                    )
                }

                // Pantalla 2: Listado de Keystores Generadas
                composable(AppRoutes.KEYSTORE_LIST) {
                    val listViewModel: KeystoreListViewModel = viewModel()
                    KeystoreListScreen(
                        viewModel = listViewModel,
                        onNavigateToGenerator = {
                            navController.navigate(AppRoutes.GENERATOR) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToDetail = { keystoreId ->
                            navController.navigate(AppRoutes.createDetailRoute(keystoreId))
                        }
                    )
                }

                // Pantalla 3: Detalle y Exportación de Keystore
                composable(
                    route = AppRoutes.KEYSTORE_DETAIL,
                    arguments = listOf(
                        navArgument("keystoreId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val keystoreId = backStackEntry.arguments?.getLong("keystoreId") ?: 0L
                    val detailViewModel: KeystoreDetailViewModel = viewModel()
                    KeystoreDetailScreen(
                        keystoreId = keystoreId,
                        viewModel = detailViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Pantalla 4: Menú de Configuración y Ajustes
                composable(AppRoutes.SETTINGS) {
                    SettingsScreen(
                        themeViewModel = themeViewModel,
                        onNavigateToColorTheme = {
                            navController.navigate(AppRoutes.COLOR_THEME)
                        }
                    )
                }

                // Pantalla 5: Personalización de Color y Apariencia
                composable(AppRoutes.COLOR_THEME) {
                    ColorPickerScreen(
                        themeViewModel = themeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
