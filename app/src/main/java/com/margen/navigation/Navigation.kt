package com.margen.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import com.margen.ui.dashboard.DashboardScreen
import com.margen.ui.history.HistoryScreen
import com.margen.ui.products.ProductDetailScreen
import com.margen.ui.products.ProductEditScreen
import com.margen.ui.products.ProductListScreen
import com.margen.ui.settings.SettingsScreen
import com.margen.ui.tools.ToolsScreen

object Routes {
    const val ARG_PRODUCT_ID = "productId"

    const val DASHBOARD = "dashboard"
    const val PRODUCTS = "products"
    const val TOOLS = "tools?productId={productId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val DETAIL = "product/{productId}"
    const val EDIT = "product/edit/{productId}"

    fun tools(productId: Long = 0L) = "tools?productId=$productId"
    fun detail(productId: Long) = "product/$productId"
    fun edit(productId: Long = 0L) = "product/edit/$productId"
}

private data class BottomTab(
    /** Ruta del destino (patrón), usada para saber cuál pestaña está activa. */
    val route: String,
    /** Ruta concreta a la que se navega al tocar la pestaña. */
    val target: String,
    val label: String,
    val icon: ImageVector
)

private val BOTTOM_TABS = listOf(
    BottomTab(Routes.DASHBOARD, Routes.DASHBOARD, "Inicio", Icons.Default.Home),
    BottomTab(Routes.PRODUCTS, Routes.PRODUCTS, "Productos", Icons.Default.ShoppingCart),
    BottomTab(Routes.TOOLS, Routes.tools(), "Calculadora", Icons.Default.Build),
    BottomTab(Routes.HISTORY, Routes.HISTORY, "Historial", Icons.AutoMirrored.Filled.List),
    BottomTab(Routes.SETTINGS, Routes.SETTINGS, "Ajustes", Icons.Default.Settings)
)

private fun NavHostController.navigateToTab(target: String) {
    navigate(target) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MargenNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = BOTTOM_TABS.any { tab ->
        destination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    BOTTOM_TABS.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.target) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNewProduct = { navController.navigate(Routes.edit()) },
                    onOpenProduct = { id -> navController.navigate(Routes.detail(id)) },
                    onOpenProducts = { navController.navigateToTab(Routes.PRODUCTS) },
                    onOpenCalculator = { navController.navigateToTab(Routes.tools()) },
                    onOpenHistory = { navController.navigateToTab(Routes.HISTORY) }
                )
            }

            composable(Routes.PRODUCTS) {
                ProductListScreen(
                    onNewProduct = { navController.navigate(Routes.edit()) },
                    onOpenProduct = { id -> navController.navigate(Routes.detail(id)) }
                )
            }

            composable(
                route = Routes.TOOLS,
                arguments = listOf(
                    navArgument(Routes.ARG_PRODUCT_ID) {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { entry ->
                ToolsScreen(productId = entry.arguments?.getLong(Routes.ARG_PRODUCT_ID) ?: 0L)
            }

            composable(Routes.HISTORY) { HistoryScreen() }

            composable(Routes.SETTINGS) { SettingsScreen() }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong(Routes.ARG_PRODUCT_ID) ?: 0L
                ProductDetailScreen(
                    productId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.edit(id)) },
                    onOpenCalculator = { navController.navigate(Routes.tools(id)) },
                    onDeleted = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.EDIT,
                arguments = listOf(navArgument(Routes.ARG_PRODUCT_ID) { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong(Routes.ARG_PRODUCT_ID) ?: 0L
                ProductEditScreen(
                    productId = id,
                    onBack = { navController.popBackStack() },
                    onSaved = { savedId, isNew ->
                        if (isNew) {
                            navController.navigate(Routes.detail(savedId)) {
                                popUpTo(Routes.EDIT) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}
