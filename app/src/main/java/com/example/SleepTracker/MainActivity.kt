// MainActivity.kt
package com.example.SleepTracker

//import com.example.SleepTracker.ml.Model
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.SleepTracker.components.BottomNavigationBar
import com.example.SleepTracker.components.NavHostContainer
import com.example.SleepTracker.data.SleepDatabase
import com.example.SleepTracker.ui.theme.SleepsafeTheme
import com.example.SleepTracker.utils.PermissionsHelper
import com.example.SleepTracker.utils.TFLiteLoader
import com.jlibrosa.audio.JLibrosa
import kotlinx.coroutines.launch


//import com.example.SleepTracker.utils.processAudio

//import com.example.SleepTracker.utils.runModel
import android.os.SystemClock


class MainActivity : ComponentActivity() {
    lateinit var permissionsHelper: PermissionsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register for permissions before creating PermissionsHelper

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Check if we need to show the exact alarm permission dialog
            if (permissions.values.all { it } && permissionsHelper.needsExactAlarmPermission()) {
                permissionsHelper.openExactAlarmSettings()
            }
        }

        // Initialize PermissionsHelper with the launcher
        permissionsHelper = PermissionsHelper(this, permissionLauncher)

        // Set initial content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setInitialContent()
        }

        // Initialize database
        lifecycleScope.launch {
            try {
                SleepDatabase.getDatabase(applicationContext)
                Log.d("MainActivity", "Database initialized successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing database", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        // Update content based on current permission state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setInitialContent()
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setInitialContent() {
        val startDestination = if (!permissionsHelper.hasRequiredPermissions()) "welcome" else "home"

        setContent {
            SleepsafeTheme (){
                if (startDestination == "home") {
                    MainScaffold(this)
                } else {
                    val navController = rememberNavController()
                    NavHostContainer(
                        navController = navController,
                        activity = this,
                        permissionsHelper = permissionsHelper,
                        modifier = Modifier,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(activity: ComponentActivity) {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "记 录 每 一 次 睡 眠",
                        fontSize = 20.sp,
                        // style = TextStyle(letterSpacing = 2.sp) // 增加字符间距
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHostContainer(
            navController = navController,
            activity = activity,
            permissionsHelper = (activity as MainActivity).permissionsHelper,
            modifier = Modifier.padding(innerPadding),
            startDestination = "home"
        )
    }
}
