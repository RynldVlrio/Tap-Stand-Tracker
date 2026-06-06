package com.taptrack.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taptrack.app.ui.screens.add.AddTapStandScreen
import com.taptrack.app.ui.screens.detail.DetailScreen
import com.taptrack.app.ui.screens.list.ListScreen
import com.taptrack.app.ui.screens.map.MapScreen

sealed class Screen(val route: String) {
    object Map : Screen("map")
    object List : Screen("list")
    object Add : Screen("add?lat={lat}&lng={lng}") {
        fun route(lat: Double? = null, lng: Double? = null) =
            if (lat != null && lng != null) "add?lat=$lat&lng=$lng" else "add"
    }
    object Edit : Screen("edit/{id}") {
        fun route(id: Long) = "edit/$id"
    }
    object Detail : Screen("detail/{id}") {
        fun route(id: Long) = "detail/$id"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(Screen.Map.route, Screen.List.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                TapTrackBottomBar(
                    currentRoute = currentRoute,
                    onMapClick = {
                        navController.navigate(Screen.Map.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddClick = { navController.navigate(Screen.Add.route()) },
                    onListClick = {
                        navController.navigate(Screen.List.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(280))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(280))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(280))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(280))
            }
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.route(id)) },
                    onNavigateToAdd = { lat, lng -> navController.navigate(Screen.Add.route(lat, lng)) }
                )
            }
            composable(Screen.List.route) {
                ListScreen(
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.route(id)) }
                )
            }
            composable(
                route = Screen.Add.route,
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("lng") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { entry ->
                val lat = entry.arguments?.getString("lat")?.toDoubleOrNull()
                val lng = entry.arguments?.getString("lng")?.toDoubleOrNull()
                AddTapStandScreen(
                    initialLat = lat, initialLng = lng, tapStandId = null,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Edit.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments!!.getLong("id")
                AddTapStandScreen(
                    initialLat = null, initialLng = null, tapStandId = id,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments!!.getLong("id")
                DetailScreen(
                    tapStandId = id,
                    onNavigateToEdit = { navController.navigate(Screen.Edit.route(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun TapTrackBottomBar(
    currentRoute: String?,
    onMapClick: () -> Unit,
    onAddClick: () -> Unit,
    onListClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                icon = Icons.Default.Map,
                label = "Map",
                selected = currentRoute == Screen.Map.route,
                onClick = onMapClick,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Tap Stand")
                }
            }

            NavBarItem(
                icon = Icons.Default.List,
                label = "List",
                selected = currentRoute == Screen.List.route,
                onClick = onListClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
