package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import com.example.ui.importzip.ZipImportScreen
import com.example.ui.importzip.ZipImportViewModel
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
    val isChildScreen = currentRoute?.startsWith("keystore_detail") == true ||
        currentRoute == AppRoutes.COLOR_THEME ||
        currentRoute == AppRoutes.ZIP_IMPORT

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
                // Pantalla 1: Formulario Generador de Keystores (Pestaña 0)
                composable(
                    route = AppRoutes.GENERATOR,
                    enterTransition = {
                        val initialOrder = AppRoutes.getBottomBarOrder(initialState.destination.route)
                        if (initialOrder > 0) {
                            // Viene de una pestaña a la derecha: entra desde la izquierda
                            slideInHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                initialOffsetX = { -it / 3 }
                            ) + fadeIn(animationSpec = tween(300))
                        } else {
                            fadeIn(animationSpec = tween(250))
                        }
                    },
                    exitTransition = {
                        val targetOrder = AppRoutes.getBottomBarOrder(targetState.destination.route)
                        if (targetOrder > 0) {
                            // Va hacia una pestaña a la derecha: sale hacia la izquierda
                            slideOutHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                targetOffsetX = { -it / 3 }
                            ) + fadeOut(animationSpec = tween(200))
                        } else {
                            fadeOut(animationSpec = tween(200))
                        }
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(250))
                    }
                ) {
                    val generatorViewModel: GeneratorViewModel = viewModel()
                    GeneratorScreen(
                        viewModel = generatorViewModel,
                        onNavigateToDetail = { keystoreId ->
                            navController.navigate(AppRoutes.createDetailRoute(keystoreId))
                        }
                    )
                }

                // Pantalla 2: Listado de Keystores Generadas (Pestaña 1)
                composable(
                    route = AppRoutes.KEYSTORE_LIST,
                    enterTransition = {
                        val initialOrder = AppRoutes.getBottomBarOrder(initialState.destination.route)
                        if (initialOrder in 0..0) {
                            // Viene del generador (izquierda): entra desde la derecha
                            slideInHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                initialOffsetX = { it / 3 }
                            ) + fadeIn(animationSpec = tween(300))
                        } else if (initialOrder > 1) {
                            // Viene de ajustes (derecha): entra desde la izquierda
                            slideInHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                initialOffsetX = { -it / 3 }
                            ) + fadeIn(animationSpec = tween(300))
                        } else {
                            fadeIn(animationSpec = tween(250))
                        }
                    },
                    exitTransition = {
                        val targetOrder = AppRoutes.getBottomBarOrder(targetState.destination.route)
                        if (targetOrder in 0..0) {
                            // Va hacia el generador (izquierda): sale hacia la derecha
                            slideOutHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                targetOffsetX = { it / 3 }
                            ) + fadeOut(animationSpec = tween(200))
                        } else if (targetOrder > 1) {
                            // Va hacia ajustes (derecha): sale hacia la izquierda
                            slideOutHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                targetOffsetX = { -it / 3 }
                            ) + fadeOut(animationSpec = tween(200))
                        } else {
                            fadeOut(animationSpec = tween(200))
                        }
                    },
                    popEnterTransition = {
                        // Al regresar desde la pantalla de detalle
                        fadeIn(animationSpec = tween(250)) + slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            initialOffsetX = { -it / 4 }
                        )
                    }
                ) {
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
                        },
                        onNavigateToZipImport = {
                            navController.navigate(AppRoutes.ZIP_IMPORT)
                        }
                    )
                }

                // Pantalla 3: Detalle y Exportación de Keystore (Pantalla de Profundidad)
                composable(
                    route = AppRoutes.KEYSTORE_DETAIL,
                    arguments = listOf(
                        navArgument("keystoreId") { type = NavType.LongType }
                    ),
                    enterTransition = {
                        // Entrada profunda desde el lado derecho con aceleración natural
                        slideInHorizontally(
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            initialOffsetX = { it }
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(200))
                    },
                    popExitTransition = {
                        // Salida hacia atrás deslizándose a la derecha
                        slideOutHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            targetOffsetX = { it }
                        ) + fadeOut(animationSpec = tween(250))
                    }
                ) { backStackEntry ->
                    val keystoreId = backStackEntry.arguments?.getLong("keystoreId") ?: 0L
                    val detailViewModel: KeystoreDetailViewModel = viewModel()
                    KeystoreDetailScreen(
                        keystoreId = keystoreId,
                        viewModel = detailViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Pantalla 4: Menú de Configuración y Ajustes (Pestaña 2)
                composable(
                    route = AppRoutes.SETTINGS,
                    enterTransition = {
                        val initialOrder = AppRoutes.getBottomBarOrder(initialState.destination.route)
                        if (initialOrder in 0..1) {
                            // Viene de Generador o Lista (izquierda): entra desde la derecha
                            slideInHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                initialOffsetX = { it / 3 }
                            ) + fadeIn(animationSpec = tween(300))
                        } else {
                            fadeIn(animationSpec = tween(250))
                        }
                    },
                    exitTransition = {
                        val targetOrder = AppRoutes.getBottomBarOrder(targetState.destination.route)
                        if (targetOrder in 0..1) {
                            // Va hacia pestañas de la izquierda: sale hacia la derecha
                            slideOutHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                targetOffsetX = { it / 3 }
                            ) + fadeOut(animationSpec = tween(200))
                        } else {
                            fadeOut(animationSpec = tween(200))
                        }
                    },
                    popEnterTransition = {
                        // Al regresar desde ColorTheme
                        fadeIn(animationSpec = tween(250)) + slideInHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            initialOffsetX = { -it / 4 }
                        )
                    }
                ) {
                    SettingsScreen(
                        themeViewModel = themeViewModel,
                        onNavigateToColorTheme = {
                            navController.navigate(AppRoutes.COLOR_THEME)
                        }
                    )
                }

                // Pantalla 5: Personalización de Color y Apariencia (Pantalla de Profundidad)
                composable(
                    route = AppRoutes.COLOR_THEME,
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            initialOffsetX = { it }
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(200))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            targetOffsetX = { it }
                        ) + fadeOut(animationSpec = tween(250))
                    }
                ) {
                    ColorPickerScreen(
                        themeViewModel = themeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Pantalla 6: Importación y Restauración de Paquete ZIP (Pantalla de Profundidad)
                composable(
                    route = AppRoutes.ZIP_IMPORT,
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            initialOffsetX = { it }
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(200))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            targetOffsetX = { it }
                        ) + fadeOut(animationSpec = tween(250))
                    }
                ) {
                    val zipImportViewModel: ZipImportViewModel = viewModel()
                    ZipImportScreen(
                        viewModel = zipImportViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToDetail = { keystoreId ->
                            navController.navigate(AppRoutes.createDetailRoute(keystoreId)) {
                                popUpTo(AppRoutes.KEYSTORE_LIST)
                            }
                        },
                        onNavigateToList = {
                            navController.navigate(AppRoutes.KEYSTORE_LIST) {
                                popUpTo(AppRoutes.KEYSTORE_LIST) { inclusive = true }
                            }
                        }
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
