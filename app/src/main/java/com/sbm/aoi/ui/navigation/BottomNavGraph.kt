package com.sbm.aoi.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sbm.aoi.ui.inspection.InspectionScreen
import com.sbm.aoi.ui.markup.MarkupScreen
import com.sbm.aoi.ui.models.ModelsScreen
import com.sbm.aoi.ui.photo.PhotoScreen
import com.sbm.aoi.ui.settings.SettingsScreen
import com.sbm.aoi.ui.theme.Background
import com.sbm.aoi.ui.theme.OnBackground
import com.sbm.aoi.ui.theme.OnSurface
import com.sbm.aoi.ui.theme.Primary
import com.sbm.aoi.ui.theme.Surface

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Inspection : Screen(
        route = "inspection",
        title = "Инспекция",
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
    )

    data object Photo : Screen(
        route = "photo",
        title = "Фото",
        selectedIcon = Icons.Filled.Photo,
        unselectedIcon = Icons.Outlined.Photo,
    )

    data object Markup : Screen(
        route = "markup",
        title = "Разметка",
        selectedIcon = Icons.Filled.Draw,
        unselectedIcon = Icons.Outlined.Draw,
    )

    data object Models : Screen(
        route = "models",
        title = "Модели",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology,
    )

    data object Settings : Screen(
        route = "settings",
        title = "Настройки",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    )
}

private val screens = listOf(
    Screen.Inspection,
    Screen.Photo,
    Screen.Markup,
    Screen.Models,
    Screen.Settings,
)

private const val ANIM_DURATION = 300

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
            ) {
                screens.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                            )
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = OnSurface,
                            unselectedTextColor = OnSurface,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Inspection.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(ANIM_DURATION))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(ANIM_DURATION))
            },
        ) {
            composable(Screen.Inspection.route) { InspectionScreen() }
            composable(Screen.Photo.route) { PhotoScreen() }
            composable(Screen.Markup.route) { MarkupScreen() }
            composable(Screen.Models.route) { ModelsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
