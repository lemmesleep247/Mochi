package io.github.mochi.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.mochi.data.model.MediaType
import io.github.mochi.ui.screens.detail.DetailScreen
import io.github.mochi.ui.screens.discover.DiscoverScreen
import io.github.mochi.ui.screens.list.ListScreen
import io.github.mochi.ui.screens.login.MalLoginScreen
import io.github.mochi.ui.screens.profile.ProfileScreen

@Composable
fun MochiNavHost(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination == null ||
        currentDestination.hasRoute(ListRoute::class) ||
        currentDestination.hasRoute(DiscoverRoute::class) ||
        currentDestination.hasRoute(ProfileRoute::class)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(ListRoute::class) == true,
                        onClick = { navController.navigateToTopLevel(ListRoute) },
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        label = { Text("My List") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(DiscoverRoute::class) == true,
                        onClick = { navController.navigateToTopLevel(DiscoverRoute) },
                        icon = { Icon(Icons.Filled.Explore, contentDescription = null) },
                        label = { Text("Discover") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(ProfileRoute::class) == true,
                        onClick = { navController.navigateToTopLevel(ProfileRoute) },
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Profile") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ListRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<ListRoute> {
                ListScreen(onOpenDetail = { id, type -> navController.navigate(DetailRoute(id, type.path)) })
            }
            composable<DiscoverRoute> {
                DiscoverScreen(onOpenDetail = { id, type -> navController.navigate(DetailRoute(id, type.path)) })
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onSignOut = onSignOut,
                    onConnectForStats = { navController.navigate(MalLoginRoute) },
                )
            }
            composable<DetailRoute> { entry ->
                val route: DetailRoute = entry.toRoute()
                DetailScreen(
                    id = route.id,
                    type = MediaType.entries.first { it.path == route.type },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<MalLoginRoute> {
                MalLoginScreen(
                    onDismiss = { navController.popBackStack() },
                    onLoginSuccess = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
