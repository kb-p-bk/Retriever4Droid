package com.ragdroid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ragdroid.app.settings.SettingsScreen
import com.ragdroid.core.ui.theme.RetrieverTheme
import com.ragdroid.feature.home.HomeScreen
import com.ragdroid.feature.ingest.IngestScreen
import com.ragdroid.feature.library.LibraryScreen
import com.ragdroid.feature.search.ChunkViewerScreen
import com.ragdroid.feature.search.SearchScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RetrieverTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RetrieverNavHost()
                }
            }
        }
    }
}

/**
 * Navigation destinations for the app.
 */
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Ingest : Screen("ingest", "Add", Icons.Default.Add)
    data object Library : Screen("library", "Library", Icons.Default.LibraryBooks)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

// Route for chunk viewer (not in bottom nav)
const val CHUNK_VIEWER_ROUTE = "chunk_viewer/{documentId}/{chunkIndex}"

private fun navigateToTab(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun RetrieverNavHost() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Ingest,
        Screen.Library,
        Screen.Settings
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedTablet = maxWidth >= 600.dp

        if (isExpandedTablet) {
            // Adaptive Tablet Layout (e.g. Samsung Galaxy Tab S9 FE+) with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    screens.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationRailItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = isSelected,
                            onClick = {
                                navigateToTab(navController, screen.route)
                            }
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onNavigateToSearch = { navigateToTab(navController, Screen.Search.route) },
                                onNavigateToIngest = { navigateToTab(navController, Screen.Ingest.route) },
                                onNavigateToLibrary = { navigateToTab(navController, Screen.Library.route) }
                            )
                        }
                        composable(Screen.Search.route) {
                            SearchScreen(
                                onNavigateToChunk = { documentId, chunkIndex ->
                                    navController.navigate("chunk_viewer/$documentId/$chunkIndex")
                                }
                            )
                        }
                        composable(CHUNK_VIEWER_ROUTE) { backStackEntry ->
                            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
                            val chunkIndex = backStackEntry.arguments?.getString("chunkIndex")?.toIntOrNull() ?: 0
                            ChunkViewerScreen(
                                documentId = documentId,
                                chunkIndex = chunkIndex,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(Screen.Ingest.route) {
                            IngestScreen()
                        }
                        composable(Screen.Library.route) {
                            LibraryScreen(
                                onNavigateToChunk = { documentId, chunkIndex ->
                                    navController.navigate("chunk_viewer/$documentId/$chunkIndex")
                                }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen()
                        }
                    }
                }
            }
        } else {
            // Compact Phone Layout with NavigationBar
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        screens.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label) },
                                selected = isSelected,
                                onClick = {
                                    navigateToTab(navController, screen.route)
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            onNavigateToSearch = { navigateToTab(navController, Screen.Search.route) },
                            onNavigateToIngest = { navigateToTab(navController, Screen.Ingest.route) },
                            onNavigateToLibrary = { navigateToTab(navController, Screen.Library.route) }
                        )
                    }
                    composable(Screen.Search.route) {
                        SearchScreen(
                            onNavigateToChunk = { documentId, chunkIndex ->
                                navController.navigate("chunk_viewer/$documentId/$chunkIndex")
                            }
                        )
                    }
                    composable(CHUNK_VIEWER_ROUTE) { backStackEntry ->
                        val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
                        val chunkIndex = backStackEntry.arguments?.getString("chunkIndex")?.toIntOrNull() ?: 0
                        ChunkViewerScreen(
                            documentId = documentId,
                            chunkIndex = chunkIndex,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Ingest.route) {
                        IngestScreen()
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen()
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}
