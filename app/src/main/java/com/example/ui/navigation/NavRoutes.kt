package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rutas de navegación de la aplicación para navegación estructurada y tipada.
 */
sealed class NavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Generator : NavDestination(
        route = "generator",
        title = "Generador",
        selectedIcon = Icons.Filled.VpnKey,
        unselectedIcon = Icons.Outlined.VpnKey
    )

    data object KeystoreList : NavDestination(
        route = "keystores",
        title = "Mis Keystores",
        selectedIcon = Icons.Filled.FolderShared,
        unselectedIcon = Icons.Outlined.FolderShared
    )
}

object AppRoutes {
    const val GENERATOR = "generator"
    const val KEYSTORE_LIST = "keystores"
    const val KEYSTORE_DETAIL = "keystore_detail/{keystoreId}"

    fun createDetailRoute(keystoreId: Long): String = "keystore_detail/$keystoreId"
}
