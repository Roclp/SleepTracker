// NavHostContainer.kt
package com.example.SleepTracker.components

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.SleepTracker.BottomNavItems
import com.example.SleepTracker.screens.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.SleepTracker.viewmodel.HomeViewModel
import com.example.SleepTracker.utils.PermissionsHelper

/**
 * A composable function to manage the navigation graph of the SleepSafe app.
 * @param navController The NavHostController to control navigation between composables.
 * @param activity The ComponentActivity instance to manage activity lifecycle events.
 * @param permissionsHelper The PermissionsHelper instance to handle permissions.
 * @param modifier A Modifier to customize the layout or appearance of this composable.
 * @param startDestination The initial route to display.
 */
@Composable
fun NavHostContainer(
    navController: NavHostController,
    activity: ComponentActivity,
    permissionsHelper: PermissionsHelper,
    modifier: Modifier = Modifier,
    startDestination: String = "welcome"
) {
    // Define the navigation graph
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Welcome Screen
        composable("welcome") {
            WelcomeScreen(
                navController = navController,
                permissionsHelper = permissionsHelper
            )
        }

        // Define navigation for each bottom navigation item
        composable(BottomNavItems[0].route) {
            val homeViewModel: HomeViewModel = viewModel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                HomeScreen(homeViewModel = homeViewModel)
            }
        }
        composable(BottomNavItems[1].route) {
            AnalysisScreen()
        }
        composable(BottomNavItems[2].route) {
            SettingsScreen()
        }
        composable(BottomNavItems[3].route) {
            AccountScreen()
        }
    }
}
